package com.platform.model.dto;

import com.platform.model.Obligation;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class UpcomingResponse {
    private List<Obligation> obligations;
    private Map<String, BigDecimal> totals;
    private List<Obligation> renewalAlerts;
}
