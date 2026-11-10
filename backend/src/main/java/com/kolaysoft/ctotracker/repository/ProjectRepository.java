package com.kolaysoft.ctotracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.kolaysoft.ctotracker.entity.Project;

/** Filtreli sorgular icin bkz. {@code repository.spec.ProjectSpecifications}. */
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
}
