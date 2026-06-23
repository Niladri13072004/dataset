package com.dataset.backend.DTO;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EventAnalysisResponse {
    private int risk;             // 0 - 100 for risk-fill width and rating
    private int delay;            // Peak delay in minutes
    private String radius;        // e.g., "1.5 km" impact zone
    private int conf;             // Model AI confidence percentage
    private int officers;         // Number of traffic personnel required
    private int barricades;       // Number of portable barricades to deploy

    // --- CHANGED LINE HERE: Swapped from String to Object/Map to accept spatial telemetry ---
    private Object routes;        // Now holds the structured bypass map layers (status, center, layers)

    private String reduction;     // e.g., "24% mitigation"
    private int vehicles;         // Estimated affected vehicle volume
    private int vms;              // Variable Message Signboards needed
    private String cost;          // e.g., "₹45,000 estimated operation cost"
    private List<String> roads;   // Array of specific road segment strings impacted
    private List<HistoricalMatchDTO> hist; // Historical similar events list
}