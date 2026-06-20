package com.dataset.backend.controller;

import com.dataset.backend.DTO.EventAnalysisRequest;
import com.dataset.backend.DTO.EventAnalysisResponse;
import com.dataset.backend.model.LiveEvent;
import com.dataset.backend.model.ResourceMetric;
import com.dataset.backend.model.TrafficAlert;
import com.dataset.backend.repository.LiveEventRepository;
import com.dataset.backend.repository.ResourceMetricRepository;
import com.dataset.backend.repository.TrafficAlertRepository;
import com.dataset.backend.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TrafficManagementController {

    @Autowired private LiveEventRepository eventRepository;
    @Autowired private TrafficAlertRepository alertRepository;
    @Autowired private ResourceMetricRepository resourceRepository;
    @Autowired private AnalysisService analysisService;

    // --- LIVE REPO ENDPOINTS ---
    @GetMapping("/events")
    public List<LiveEvent> getAllEvents() {
        return eventRepository.findAll();
    }

    @GetMapping("/alerts")
    public List<TrafficAlert> getAllAlerts() {
        return alertRepository.findAll();
    }

    @GetMapping("/resources")
    public List<ResourceMetric> getMetrics() {
        return resourceRepository.findAll();
    }

    // --- PUT METHOD TO DISMISS ALERTS ---
    @PutMapping("/alerts/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        alertRepository.findById(id).ifPresent(alert -> {
            alert.setRead(true);
            alertRepository.save(alert);
        });
        return ResponseEntity.ok().build();
    }

    // --- UPDATED PREDICTIVE SIMULATION CONTROL LINK ---
    @PostMapping("/events/analyze")
    public ResponseEntity<EventAnalysisResponse> runPredictiveAnalysis(@RequestBody EventAnalysisRequest requestData) {
        // Automatically capture and delegate full front-end DTO mappings to your updated AnalysisService
        EventAnalysisResponse result = analysisService.calculateImpact(requestData);
        return ResponseEntity.ok(result);
    }
}