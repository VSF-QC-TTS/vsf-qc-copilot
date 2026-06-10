package me.nghlong3004.vqc.api.rubric.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, Long> {

  Page<RubricCriterion> findByRubricVersion(RubricVersion rubricVersion, Pageable pageable);

  List<RubricCriterion> findByRubricVersionOrderBySortOrderAscIdAsc(RubricVersion rubricVersion);

  long countByRubricVersion(RubricVersion rubricVersion);

  boolean existsByRubricVersionAndMetricKey(RubricVersion rubricVersion, String metricKey);

  boolean existsByRubricVersionAndMetricKeyAndPublicIdNot(
      RubricVersion rubricVersion, String metricKey, UUID publicId);

  Optional<RubricCriterion> findByPublicIdAndRubricVersionRubricCreatedBy(
      UUID publicId, User createdBy);
}
