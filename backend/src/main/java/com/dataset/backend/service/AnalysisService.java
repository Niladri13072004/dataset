package com.dataset.backend.service;

import com.dataset.backend.DTO.EventAnalysisRequest;
import com.dataset.backend.DTO.EventAnalysisResponse;
import com.dataset.backend.DTO.HistoricalMatchDTO;
import com.dataset.backend.model.LiveEvent;
import com.dataset.backend.repository.LiveEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    private final RestClient restClient;
    private final LiveEventRepository liveEventRepository;

    public AnalysisService(LiveEventRepository liveEventRepository) {
        this.liveEventRepository = liveEventRepository;
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8000")
                .build();
    }

    public EventAnalysisResponse calculateImpact(EventAnalysisRequest request) {
        try {
            // 1. Fetch live predictive classes from the Python microservice models
            Map<?, ?> mlResult = restClient.post()
                    .uri("/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (mlResult != null && "success".equals(mlResult.get("status"))) {
                String priority = String.valueOf(mlResult.get("priority"));
                String roadClosure = String.valueOf(mlResult.get("roadClosureRequired"));

                boolean isHigh = "High".equalsIgnoreCase(priority);
                boolean needsClosure = "Yes".equalsIgnoreCase(roadClosure);

                // 2. Compute dynamic operational metrics
                int riskScore = isHigh ? 70 : 40;
                if (needsClosure) riskScore += 15;
                if (request.getAttendance() > 50000) riskScore += 15;
                if (riskScore > 100) riskScore = 100;

                int delayMinutes = isHigh ? 30 : 10;
                if (needsClosure) delayMinutes += 20;
                if (request.getAttendance() > 100000) delayMinutes += 12;

                int officersNeeded = (request.getAttendance() / 5000) + (isHigh ? 8 : 3);
                if (needsClosure) officersNeeded += 6;

                int barricadesNeeded = needsClosure ? 20 : 4;

                // Centralize Congestion Mapping logic to guarantee alignment
                String congestionLevel = riskScore >= 75 ? "SEVERE" : riskScore >= 45 ? "HIGH" : "MEDIUM";

                // 3. Persist the record inside your relational schema
                LiveEvent liveEvent = new LiveEvent();
                liveEvent.setName(request.getNotes() != null && !request.getNotes().isEmpty() ? request.getNotes() : "Event Run: " + request.getType());
                liveEvent.setLocation(request.getLocation());
                liveEvent.setType(request.getType().toUpperCase());
                liveEvent.setStatus("PRE_EVENT");
                liveEvent.setAttendance(request.getAttendance());

                // FIXED: Now perfectly reflects the calculated riskScore metrics tier
                liveEvent.setCongestionLevel(congestionLevel);
                liveEvent.setOfficersRequired(officersNeeded);
                liveEvent.setOfficersDeployed(0);
                liveEvent.setEtaClear(delayMinutes + " mins");

                // FIXED: Extract latitude and longitude dynamically from the incoming request object
                // instead of relying on hardcoded location-name strings.
                if (request.getLatitude() != 0.0 && request.getLongitude() != 0.0) {
                    liveEvent.setLatitude(request.getLatitude());
                    liveEvent.setLongitude(request.getLongitude());
                } else {
                    // Fallback to defaults only if request completely lacks geo-data
                    liveEvent.setLatitude(12.9716);
                    liveEvent.setLongitude(77.5946);
                }

                liveEventRepository.save(liveEvent);

                List<String> impactedRoads = determineImpactedRoads(request.getLocation());
                List<HistoricalMatchDTO> historicalGrid = generateHistoricalMatches(request.getType());

                return EventAnalysisResponse.builder()
                        .risk(riskScore)
                        .delay(delayMinutes)
                        .radius(riskScore >= 75 ? "3.5 km" : "1.8 km")
                        .conf(isHigh ? 94 : 88)
                        .officers(officersNeeded)
                        .barricades(barricadesNeeded)
                        .routes("Alternative bypass vectors activated via peripheral corridors.")
                        .reduction("35% expected congestion mitigation under active assignment")
                        .vehicles(isHigh ? 6 : 3)
                        .vms(isHigh ? 5 : 2)
                        .cost(riskScore >= 75 ? "₹1.8L" : "₹60K")
                        .roads(impactedRoads)
                        .hist(historicalGrid)
                        .build();
            } else {
                throw new RuntimeException("ML pipeline returned structural error code.");
            }

        } catch (Exception e) {
            // FIXED: If Python goes offline, fallback to a sensible "MEDIUM" baseline instead of leaving DB out of sync
            return EventAnalysisResponse.builder()
                    .risk(35)
                    .delay(15)
                    .radius("1.2 km")
                    .conf(50)
                    .officers(5)
                    .barricades(2)
                    .routes("Fallback localized handling active.")
                    .reduction("0%")
                    .roads(Arrays.asList("ML Core Engine Offline"))
                    .hist(new ArrayList<>())
                    .build();
        }
    }

    private List<String> determineImpactedRoads(String location) {
        if (location != null && location.toLowerCase().contains("mg road")) {
            return Arrays.asList("MG Road", "Brigade Rd", "Residency Rd", "St Marks Rd", "Richmond Rd");
        }
        return Arrays.asList("Primary Access Route Link", "Perimeter Intersections");
    }

    private List<HistoricalMatchDTO> generateHistoricalMatches(String type) {
        String eventType = type != null ? type.toLowerCase() : "political";
        return Arrays.asList(
                HistoricalMatchDTO.builder()
                        .pill(eventType)
                        .label(eventType.toUpperCase())
                        .name("Comparable Regional Dataset Run")
                        .loc("Zone Spatial Coordinate Reference Grid")
                        .delay(34)
                        .crowd("40k")
                        .officers(12)
                        .date("Baseline History Instance")
                        .acc("92.1%")
                        .build()
        );
    }
}