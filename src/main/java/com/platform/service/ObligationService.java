package com.platform.service;

import com.platform.exception.BusinessException;
import com.platform.model.*;
import com.platform.model.dto.*;
import com.platform.repository.ObligationRepository;
import com.platform.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ObligationService {

    private final ObligationRepository obligationRepository;
    private final PaymentRepository paymentRepository;
    private final SseService sseService;

    @Transactional
    public ObligationResponse createObligation(ObligationRequest request) {
        // Проверка на дублирование активного обязательства с таким же названием
        Optional<Obligation> existingActive = obligationRepository
                .findByTitleIgnoreCaseAndStatus(request.getTitle(), Status.ACTIVE);

        // Определяем статус
        Status status = request.getNextPaymentDate().isBefore(LocalDate.now())
                ? Status.EXPIRED
                : Status.ACTIVE;

        // Создаём обязательство
        Obligation obligation = Obligation.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .category(request.getCategory())
                .recurrence(request.getRecurrence())
                .nextPaymentDate(request.getNextPaymentDate())
                .status(status)
                .build();

        Obligation saved = obligationRepository.save(obligation);

        // Формируем ответ
        ObligationResponse response = ObligationResponse.builder()
                .obligation(saved)
                .build();

        if (existingActive.isPresent()) {
            response.setWarning("Активное обязательство с таким названием уже существует");
        }

        return response;
    }

    @Transactional
    public List<Obligation> getObligations(Status status, Category category) {
        List<Obligation> obligations;

        if (status != null && category != null) {
            obligations = obligationRepository.findByStatusAndCategory(status, category);
        } else if (status != null) {
            obligations = obligationRepository.findByStatus(status);
        } else if (category != null) {
            obligations = obligationRepository.findByCategory(category);
        } else {
            obligations = obligationRepository.findAll();
        }

        List<Obligation> updated = applyLazyExpiry(obligations);
        updated.sort(Comparator.comparing(Obligation::getNextPaymentDate));
        return updated;
    }

    @Transactional
    public List<Obligation> applyLazyExpiry(List<Obligation> obligations) {
        LocalDate today = LocalDate.now();
        List<Obligation> updated = new ArrayList<>();

        for (Obligation obligation : obligations) {
            // Разовые платежи (recurrence == null) со статусом ACTIVE и просроченной датой
            if (obligation.getStatus() == Status.ACTIVE &&
                    obligation.getRecurrence() == null &&
                    obligation.getNextPaymentDate().isBefore(today)) {
                obligation.setStatus(Status.EXPIRED);
                updated.add(obligationRepository.save(obligation));
            } else {
                updated.add(obligation);
            }
        }

        return updated;
    }

    @Transactional
    public UpcomingResponse getUpcomingObligations(Integer days) {
        if (days == null) {
            days = 7;
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        List<Obligation> obligations = obligationRepository
                .findByNextPaymentDateBetweenAndStatusOrderByNextPaymentDateAsc(today, endDate, Status.ACTIVE);

        // Применяем lazy expiry
        obligations = applyLazyExpiry(obligations);

        // Фильтруем только активные после обновления
        List<Obligation> activeObligations = obligations.stream()
                .filter(o -> o.getStatus() == Status.ACTIVE)
                .collect(Collectors.toList());

        // Вычисляем суммы по валютам
        Map<String, BigDecimal> totals = activeObligations.stream()
                .collect(Collectors.groupingBy(
                        Obligation::getCurrency,
                        Collectors.reducing(BigDecimal.ZERO,
                                Obligation::getAmount,
                                BigDecimal::add)
                ));

        // Формируем алерты по подпискам
        List<Obligation> renewalAlerts = activeObligations.stream()
                .filter(o -> o.getCategory() == Category.SUBSCRIPTION &&
                        o.getRecurrence() != null)
                .collect(Collectors.toList());

        return UpcomingResponse.builder()
                .obligations(activeObligations)
                .totals(totals)
                .renewalAlerts(renewalAlerts)
                .build();
    }

    @Transactional
    public PaymentResponse processPayment(UUID obligationId, PaymentRequest request) {
        Obligation obligation = obligationRepository.findById(obligationId)
                .orElseThrow(() -> new BusinessException("Обязательство не найдено"));

        // Проверяем статус
        if (obligation.getStatus() != Status.ACTIVE) {
            throw new BusinessException("Невозможно оплатить: статус обязательства " + obligation.getStatus());
        }

        // Создаём запись оплаты
        Payment payment = Payment.builder()
                .obligationId(obligation.getId())
                .amount(request.getAmount() != null ? request.getAmount() : obligation.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : obligation.getCurrency())
                .paidAt(java.time.LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Обновляем обязательство в зависимости от периодичности
        if (obligation.getRecurrence() != null) {
            // Рекуррентное обязательство - сдвигаем дату
            LocalDate newDate = calculateNextDate(
                    obligation.getNextPaymentDate(),
                    obligation.getRecurrence()
            );
            obligation.setNextPaymentDate(newDate);
            // Статус остаётся ACTIVE
        } else {
            // Разовое обязательство - закрываем
            obligation.setStatus(Status.CANCELLED);
        }

        Obligation updatedObligation = obligationRepository.save(obligation);

        // Отправляем SSE событие
        sseService.sendObligationUpdate(updatedObligation);

        return PaymentResponse.builder()
                .obligation(updatedObligation)
                .payment(savedPayment)
                .build();
    }

    private LocalDate calculateNextDate(LocalDate currentDate, Recurrence recurrence) {
        return switch (recurrence) {
            case MONTHLY -> currentDate.plusMonths(1);
            case QUARTERLY -> currentDate.plusMonths(3);
            case YEARLY -> currentDate.plusYears(1);
        };
    }

    @Transactional
    public void deleteObligation(UUID id) {
        obligationRepository.deleteById(id);
        sseService.sendObligationDeletion(id);
    }

    @Transactional
    public Obligation cancelObligation(UUID id) {
        Obligation obligation = obligationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Обязательство не найдено"));

        if (obligation.getStatus() != Status.ACTIVE) {
            throw new BusinessException(
                    "Невозможно отменить: статус обязательства " + obligation.getStatus());
        }

        obligation.setStatus(Status.CANCELLED);
        return obligationRepository.save(obligation);
    }
}
