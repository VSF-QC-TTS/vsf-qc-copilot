package me.nghlong3004.vqc.api.evaluation.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, Long> {

  /**
   * Finds an evaluation run by its public identifier.
   *
   * @param publicId public run identifier
   * @return {@link Optional} containing the matching {@link EvaluationRun} when present
   */
  Optional<EvaluationRun> findByPublicId(UUID publicId);

  /**
   * Finds evaluation runs for a project created by a specific user.
   *
   * @param projectId internal project identifier
   * @param createdBy creator {@link User}
   * @param pageable page and sort request
   * @return page of matching {@link EvaluationRun} entities
   */
  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"dataset", "rubricVersion", "rubricVersion.rubric", "targetApiConnector", "judgeModel", "job"})
  Page<EvaluationRun> findByProjectIdAndCreatedBy(Long projectId, User createdBy, Pageable pageable);

  /**
   * Finds an evaluation run by public id and creator.
   *
   * @param publicId public run identifier
   * @param createdBy creator {@link User}
   * @return {@link Optional} containing the matching {@link EvaluationRun} when present
   */
  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"dataset", "rubricVersion", "rubricVersion.rubric", "targetApiConnector", "judgeModel", "job", "project"})
  Optional<EvaluationRun> findByPublicIdAndCreatedBy(UUID publicId, User createdBy);
}
