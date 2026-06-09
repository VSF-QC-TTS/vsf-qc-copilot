package me.nghlong3004.vqc.api.project.mapper;

import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.response.ProjectCreatorResponse;
import me.nghlong3004.vqc.api.project.response.ProjectListItemResponse;
import me.nghlong3004.vqc.api.project.response.ProjectResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import org.mapstruct.Mapper;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Mapper(componentModel = "spring")
public interface ProjectMapper {

  /**
   * Maps an internal {@link Project} entity to a public API response.
   *
   * @param project internal {@link Project} entity
   * @return public {@link ProjectResponse}
   */
  ProjectResponse toResponse(Project project);

  /**
   * Maps an internal {@link Project} entity to a public list item response.
   *
   * @param project internal {@link Project} entity
   * @return public {@link ProjectListItemResponse}
   */
  ProjectListItemResponse toListItemResponse(Project project);

  /**
   * Maps a project creator {@link User} to a public nested response.
   *
   * @param user internal creator {@link User} entity
   * @return public {@link ProjectCreatorResponse}
   */
  ProjectCreatorResponse toCreatorResponse(User user);
}
