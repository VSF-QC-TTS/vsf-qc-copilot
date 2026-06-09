package me.nghlong3004.vqc.api.project.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.enums.ProjectStatus;
import me.nghlong3004.vqc.api.project.mapper.ProjectMapper;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.project.request.CreateProjectRequest;
import me.nghlong3004.vqc.api.project.response.ProjectResponse;
import me.nghlong3004.vqc.api.project.service.ProjectService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
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
    User creator =
        userRepository
            .findByUsername(normalizeEmail(username))
            .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
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
