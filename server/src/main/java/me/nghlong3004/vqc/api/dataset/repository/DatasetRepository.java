package me.nghlong3004.vqc.api.dataset.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface DatasetRepository extends JpaRepository<Dataset, Long> {

  /**
   * Finds datasets under a {@link Project}.
   *
   * @param project owner {@link Project}
   * @param pageable page and sort request
   * @return page of matching {@link Dataset} entities
   */
  Page<Dataset> findByProject(Project project, Pageable pageable);

  /**
   * Finds datasets under a {@link Project} with a matching {@link DatasetStatus}.
   *
   * @param project owner {@link Project}
   * @param status dataset status filter
   * @param pageable page and sort request
   * @return page of matching {@link Dataset} entities
   */
  Page<Dataset> findByProjectAndStatus(Project project, DatasetStatus status, Pageable pageable);

  /**
   * Finds a dataset by public id and creator.
   *
   * @param publicId public dataset identifier
   * @param createdBy creator {@link User}
   * @return {@link Optional} containing the matching {@link Dataset} when present
   */
  Optional<Dataset> findByPublicIdAndCreatedBy(UUID publicId, User createdBy);
}
