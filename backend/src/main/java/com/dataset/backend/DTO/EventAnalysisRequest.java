package com.dataset.backend.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventAnalysisRequest {
    private String type;
    private String location;
    private Integer attendance;
    private String notes;
    private Double latitude;
    private Double longitude;

    @JsonProperty("startDate") // Ensures correct matching case over the wire
    private String startDate;

    @JsonProperty("duration")
    private String duration;
}