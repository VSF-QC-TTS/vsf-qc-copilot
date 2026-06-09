package me.nghlong3004.vqc.api.project.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.enums.ProjectStatus;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

  /**
   * Finds projects created by a {@link User}.
   *
   * @param createdBy creator {@link User}
   * @param pageable page and sort request
   * @return page of matching {@link Project} entities
   */
  Page<Project> findByCreatedBy(User createdBy, Pageable pageable);

  /**
   * Finds projects created by a {@link User} with a matching {@link ProjectStatus}.
   *
   * @param createdBy creator {@link User}
   * @param status project status filter
   * @param pageable page and sort request
   * @return page of matching {@link Project} entities
   */
  Page<Project> findByCreatedByAndStatus(User createdBy, ProjectStatus status, Pageable pageable);

  /**
   * Finds a project by public id and creator.
   *
   * @param publicId public project identifier
   * @param createdBy creator {@link User}
   * @return {@link Optional} containing the matching {@link Project} when present
   */
  Optional<Project> findByPublicIdAndCreatedBy(UUID publicId, User createdBy);
}
