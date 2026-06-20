package com.dataset.backend.model;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
    @Entity
    @Table(name = "live_events")
    public class LiveEvent {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String name;              // e.g., "INC Mahapadav Rally", "IPL Match: RCB vs MI"

        private String location;          // e.g., "MG Road · Cubbon Park"
        private String type;              // e.g., "POLITICAL", "SPORTS", "CONSTRUCTION"
        private String status;            // e.g., "ONGOING", "PRE_EVENT"
        private int attendance;           // e.g., 187000
        private String congestionLevel;   // e.g., "SEVERE", "HIGH", "MEDIUM"
        private int officersDeployed;
        private int officersRequired;
        private String etaClear;          // e.g., "19:30", "3 days"

        // Geolocation coordinates for Leaflet Map mapping
        private double latitude;          // e.g., 12.9716
        private double longitude;         // e.g., 77.5946
}
