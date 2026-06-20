package com.dataset.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "resource_metrics")
public class ResourceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String resourceLabel;  // e.g., "Officers on duty", "Traffic barricades"
    private int currentUsage;      // e.g., 47
    private int totalCapacity;     // e.g., 60
    private String systemStatus;   // e.g., "All clear", "Near Capacity"
    private String uiColorClass;   // Store CSS or hex variables (e.g., "--cyan", "--danger")

   }