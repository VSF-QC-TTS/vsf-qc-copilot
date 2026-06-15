package me.nghlong3004.vqc.api.rubric.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.rubric.entity.Rubric;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface RubricVersionRepository extends JpaRepository<RubricVersion, Long> {

  Page<RubricVersion> findByRubric(Rubric rubric, Pageable pageable);

  Page<RubricVersion> findByRubricAndStatus(
      Rubric rubric, RubricVersionStatus status, Pageable pageable);

  Optional<RubricVersion> findTopByRubricOrderByVersionDesc(Rubric rubric);

  Optional<RubricVersion> findTopByRubricAndStatusAndPublicIdNotOrderByVersionDesc(
      Rubric rubric, RubricVersionStatus status, UUID publicId);

  Optional<RubricVersion> findByPublicIdAndRubricCreatedBy(UUID publicId, User createdBy);

  Page<RubricVersion> findByRubricCreatedBy(User createdBy, Pageable pageable);

  Page<RubricVersion> findByRubricCreatedByAndStatus(
      User createdBy, RubricVersionStatus status, Pageable pageable);
}
