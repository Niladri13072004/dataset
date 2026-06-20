package com.dataset.backend.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventAnalysisRequest {
    private String type;
    private String location;
    private int attendance;
    private String duration;
    private String startDate;
    private String notes;

    private double latitude;
    private double longitude;
}