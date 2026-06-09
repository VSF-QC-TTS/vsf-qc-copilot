package me.nghlong3004.vqc.api.project.service;

import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.request.CreateProjectRequest;
import me.nghlong3004.vqc.api.project.response.ProjectResponse;

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
}
