package com.kolaysoft.ctotracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kolaysoft.ctotracker.entity.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
