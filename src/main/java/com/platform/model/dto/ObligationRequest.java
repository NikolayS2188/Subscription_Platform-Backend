package com.platform.model.dto;

import jakarta.validation.constraints.*;
import com.platform.model.Category;
import com.platform.model.Recurrence;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ObligationRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title max length 255")
    private String title;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(RUB|USD|EUR)$", message = "Currency must be RUB, USD or EUR")
    private String currency;

    @NotNull(message = "Category is required")
    private Category category;

    private Recurrence recurrence;

    @NotNull(message = "Next payment date is required")
    @FutureOrPresent(message = "Next payment date must be today or in future")
    private LocalDate nextPaymentDate;
}
