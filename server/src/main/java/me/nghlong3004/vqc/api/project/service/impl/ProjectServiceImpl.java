package me.nghlong3004.vqc.api.project.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.enums.ProjectStatus;
import me.nghlong3004.vqc.api.project.mapper.ProjectMapper;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.project.request.CreateProjectRequest;
import me.nghlong3004.vqc.api.project.request.UpdateProjectRequest;
import me.nghlong3004.vqc.api.project.response.ProjectListItemResponse;
import me.nghlong3004.vqc.api.project.response.ProjectPageResponse;
import me.nghlong3004.vqc.api.project.response.ProjectResponse;
import me.nghlong3004.vqc.api.project.service.ProjectService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

  private static final int DEFAULT_RETENTION_DAYS = 30;

  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final ProjectMapper projectMapper;

  @Override
  @Transactional
  public ProjectResponse createProject(CreateProjectRequest request, String username) {
    User creator = findCreator(username);
    Project project =
        Project.builder()
            .name(request.name().trim())
            .description(trimToNull(request.description()))
            .evaluationScope(trimToNull(request.evaluationScope()))
            .retentionDays(resolveRetentionDays(request.retentionDays()))
            .status(ProjectStatus.ACTIVE)
            .createdBy(creator)
            .build();

    Project saved = projectRepository.save(project);
    log.info("Created project {} by user {}", saved.getPublicId(), creator.getPublicId());
    return projectMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public ProjectPageResponse listProjects(ProjectStatus status, Pageable pageable, String username) {
    User creator = findCreator(username);
    Page<Project> projects =
        status == null
            ? projectRepository.findByCreatedBy(creator, pageable)
            : projectRepository.findByCreatedByAndStatus(creator, status, pageable);
    List<ProjectListItemResponse> items =
        projects.getContent().stream().map(projectMapper::toListItemResponse).toList();
    return new ProjectPageResponse(
        items,
        projects.getNumber(),
        projects.getSize(),
        projects.getTotalElements(),
        projects.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public ProjectResponse getProject(UUID publicId, String username) {
    Project project = findProject(publicId, username);
    return projectMapper.toResponse(project);
  }

  @Override
  @Transactional
  public ProjectResponse updateProject(
      UUID publicId, UpdateProjectRequest request, String username) {
    Project project = findProject(publicId, username);
    if (request.name() != null) {
      project.setName(request.name().trim());
    }
    if (request.description() != null) {
      project.setDescription(trimToNull(request.description()));
    }
    if (request.evaluationScope() != null) {
      project.setEvaluationScope(trimToNull(request.evaluationScope()));
    }
    if (request.retentionDays() != null) {
      project.setRetentionDays(request.retentionDays());
    }
    Project saved = projectRepository.save(project);
    return projectMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void archiveProject(UUID publicId, String username) {
    Project project = findProject(publicId, username);
    project.setStatus(ProjectStatus.ARCHIVED);
    if (project.getArchivedAt() == null) {
      project.setArchivedAt(OffsetDateTime.now());
    }
    projectRepository.save(project);
  }

  private Project findProject(UUID publicId, String username) {
    User creator = findCreator(username);
    return projectRepository
        .findByPublicIdAndCreatedBy(publicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.PROJECT_NOT_FOUND));
  }

  private User findCreator(String username) {
    return userRepository
        .findByUsername(normalizeEmail(username))
        .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase();
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private Integer resolveRetentionDays(Integer retentionDays) {
    return retentionDays == null ? DEFAULT_RETENTION_DAYS : retentionDays;
  }
}
