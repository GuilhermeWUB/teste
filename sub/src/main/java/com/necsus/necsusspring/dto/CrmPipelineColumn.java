package com.necsus.necsusspring.dto;

import java.util.List;

public record CrmPipelineColumn(
        String name,
        String helper,
        String badge,
        String badgeClass,
        List<CrmDeal> deals
) {
}
