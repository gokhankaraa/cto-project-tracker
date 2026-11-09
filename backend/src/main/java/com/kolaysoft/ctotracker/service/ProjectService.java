package com.kolaysoft.ctotracker.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.common.exception.ResourceInUseException;
import com.kolaysoft.ctotracker.common.exception.ResourceNotFoundException;
import com.kolaysoft.ctotracker.dto.ProjectRequest;
import com.kolaysoft.ctotracker.dto.ProjectResponse;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
import com.kolaysoft.ctotracker.repository.UserRepository;
import com.kolaysoft.ctotracker.repository.WeeklyReportRepository;
import com.kolaysoft.ctotracker.repository.spec.ProjectSpecifications;
import com.kolaysoft.ctotracker.security.CurrentUser;
import org.springframework.data.domain.Sort;

/**
 * Proje CRUD is mantigi. Controller HTTP ile, repository veritabani ile ilgilenir;
 * kurallar ve entity<->DTO donusumu burada yasar.
 */
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final CurrentUser currentUser;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository,
                          WeeklyReportRepository weeklyReportRepository, CurrentUser currentUser) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll() {
        // Proje yoneticisi yalnizca kendi projelerini gorur (bkz. Viewer).
        Long visibleOwner = currentUser.viewer().visibleOwnerId();
        return projectRepository.findAll(ProjectSpecifications.ownedBy(visibleOwner), Sort.by("id")).stream()
                .map(ProjectService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        return toResponse(getProjectOrThrow(id));
    }

    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project();
        applyRequest(project, request);
        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = getProjectOrThrow(id);
        applyRequest(project, request);
        return toResponse(projectRepository.save(project));
    }

    public void delete(Long id) {
        getProjectOrThrow(id);
        // Raporu olan proje silinemez: kazara toplu silmeyi onlemek icin anlamli 409 doner.
        if (weeklyReportRepository.existsByProjectId(id)) {
            throw new ResourceInUseException(
                    "Bu projenin haftalik raporlari var; once raporlari silin.");
        }
        projectRepository.deleteById(id);
    }

    private void applyRequest(Project project, ProjectRequest request) {
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanici", request.ownerId()));
        project.setName(request.name());
        project.setCustomer(request.customer());
        project.setDescription(request.description());
        project.setStatus(request.status());
        project.setOwner(owner);
    }

    /** Proje yoksa ya da oturumdaki kullanici goremiyorsa ayni 404 (projenin varligi sizmaz). */
    private Project getProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .filter(project -> currentUser.viewer().canSee(project))
                .orElseThrow(() -> new ResourceNotFoundException("Proje", id));
    }

    private static ProjectResponse toResponse(Project p) {
        User owner = p.getOwner();
        return new ProjectResponse(
                p.getId(),
                p.getName(),
                p.getCustomer(),
                p.getDescription(),
                p.getStatus(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null);
    }
}
