package me.nghlong3004.vqc.api.requirement.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import me.nghlong3004.vqc.api.requirement.enums.RequirementStatus;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface BusinessRequirementRepository extends JpaRepository<BusinessRequirement, Long> {

  /**
   * Finds requirements under a {@link Project}.
   *
   * @param project owner {@link Project}
   * @param pageable page and sort request
   * @return page of matching {@link BusinessRequirement} entities
   */
  Page<BusinessRequirement> findByProject(Project project, Pageable pageable);

  /**
   * Finds requirements under a {@link Project} with a matching {@link RequirementStatus}.
   *
   * @param project owner {@link Project}
   * @param status requirement status filter
   * @param pageable page and sort request
   * @return page of matching {@link BusinessRequirement} entities
   */
  Page<BusinessRequirement> findByProjectAndStatus(
      Project project, RequirementStatus status, Pageable pageable);

  /**
   * Finds a requirement by public id and creator.
   *
   * @param publicId public requirement identifier
   * @param createdBy creator {@link User}
   * @return {@link Optional} containing the matching {@link BusinessRequirement} when present
   */
  Optional<BusinessRequirement> findByPublicIdAndCreatedBy(UUID publicId, User createdBy);
}
