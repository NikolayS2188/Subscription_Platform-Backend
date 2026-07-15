package com.platform.repository;

import com.platform.model.Category;
import com.platform.model.Obligation;
import com.platform.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObligationRepository extends JpaRepository<Obligation, UUID> {

    Optional<Obligation> findByTitleIgnoreCaseAndStatus(String title, Status status);

    List<Obligation> findByStatus(Status status);

    List<Obligation> findByCategory(Category category);

    List<Obligation> findByStatusAndCategory(Status status, Category category);

    List<Obligation> findByNextPaymentDateBetweenAndStatusOrderByNextPaymentDateAsc(
            LocalDate start, LocalDate end, Status status);
}
