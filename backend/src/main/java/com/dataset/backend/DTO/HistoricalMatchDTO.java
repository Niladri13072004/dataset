package com.dataset.backend.DTO;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class HistoricalMatchDTO {
    private String pill;       // CSS styling category class (e.g., "political", "sports")
    private String label;      // Human-readable type string (e.g., "Political Rally")
    private String name;       // Name of historic event match
    private String loc;        // Location details
    private int delay;         // Historical peak delay min
    private String crowd;      // Historical attendance string format (e.g., "45K")
    private int officers;      // Officers utilized historically
    private String date;       // Date of historic record occurrence
    private String acc;        // Accuracy metric text (e.g., "94.2%")
}