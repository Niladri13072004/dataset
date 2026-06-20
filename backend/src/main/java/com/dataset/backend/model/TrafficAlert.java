package com.dataset.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "traffic_alerts")
public class TrafficAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;       // e.g., "MG Road gridlock — cascading delay"

    @Column(length = 500)
    private String description; // Detailed warning message text
    private String alertType;   // CRITICAL, WARNING, INFO
    private String source;      // e.g., "sensor cluster MG-07", "AI anomaly engine"
    private String actionText;  // e.g., "Deploy diversion B3 →"
    private boolean isRead = false;
    private LocalDateTime timestamp = LocalDateTime.now();

    }