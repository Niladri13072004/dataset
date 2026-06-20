package com.dataset.backend.repository;

import com.dataset.backend.model.TrafficAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrafficAlertRepository extends JpaRepository<TrafficAlert, Long> {
   }
