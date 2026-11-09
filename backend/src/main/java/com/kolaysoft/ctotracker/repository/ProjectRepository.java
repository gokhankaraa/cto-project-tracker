package com.kolaysoft.ctotracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.kolaysoft.ctotracker.entity.Project;

/** Filtreli sorgular icin bkz. {@code repository.spec.ProjectSpecifications} (T13). */
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    /** Bir proje yoneticisine atanmis projeler (on analiz H-02). */
    List<Project> findByOwnerId(Long ownerId);
}
