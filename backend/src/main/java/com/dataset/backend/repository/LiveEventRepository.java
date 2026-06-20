package com.dataset.backend.repository;

import com.dataset.backend.model.LiveEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiveEventRepository extends JpaRepository<LiveEvent, Long> {}