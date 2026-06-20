package com.dataset.backend.repository;

import com.dataset.backend.model.ResourceMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceMetricRepository extends JpaRepository<ResourceMetric, Long> {}
