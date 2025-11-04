package com.necsus.necsusspring.dto;

import java.util.List;
import java.util.Map;

public record EventBoardSnapshot(
        List<EventBoardCardDto> cards,
        Map<String, List<EventBoardCardDto>> eventsByStatus,
        Map<String, Long> counters
) {
    public long totalEvents() {
        return cards != null ? cards.size() : 0;
    }
}
