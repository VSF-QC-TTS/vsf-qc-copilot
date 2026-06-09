package me.nghlong3004.vqc.api.project.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.enums.ProjectStatus;
import me.nghlong3004.vqc.api.project.request.CreateProjectRequest;
import me.nghlong3004.vqc.api.project.request.UpdateProjectRequest;
import me.nghlong3004.vqc.api.project.response.ProjectPageResponse;
import me.nghlong3004.vqc.api.project.response.ProjectResponse;
import org.springframework.data.domain.Pageable;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface ProjectService {

  /**
   * Creates a new {@link Project} for the authenticated user.
   *
   * @param request validated create project request
   * @param username normalized or raw username from the authenticated principal
   * @return created public {@link ProjectResponse}
   */
  ProjectResponse createProject(CreateProjectRequest request, String username);

  /**
   * Lists projects owned by the authenticated user.
   *
   * @param status optional project status filter
   * @param pageable page and sort request
   * @param username normalized or raw username from the authenticated principal
   * @return paginated public {@link ProjectPageResponse}
   */
  ProjectPageResponse listProjects(ProjectStatus status, Pageable pageable, String username);

  /**
   * Gets a project owned by the authenticated user.
   *
   * @param publicId public project identifier
   * @param username normalized or raw username from the authenticated principal
   * @return public {@link ProjectResponse}
   */
  ProjectResponse getProject(UUID publicId, String username);

  /**
   * Updates a project owned by the authenticated user.
   *
   * @param publicId public project identifier
   * @param request validated update project request
   * @param username normalized or raw username from the authenticated principal
   * @return updated public {@link ProjectResponse}
   */
  ProjectResponse updateProject(UUID publicId, UpdateProjectRequest request, String username);
}
