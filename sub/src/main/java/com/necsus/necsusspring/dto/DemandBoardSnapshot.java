package com.necsus.necsusspring.dto;

import java.util.List;
import java.util.Map;

public record DemandBoardSnapshot(
        List<DemandBoardCardDto> cards,
        Map<String, List<DemandBoardCardDto>> demandsByStatus,
        Map<String, Long> counters
) {

    public long totalDemands() {
        return cards != null ? cards.size() : 0;
    }
}
