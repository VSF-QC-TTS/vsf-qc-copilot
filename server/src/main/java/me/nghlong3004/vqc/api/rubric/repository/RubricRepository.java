package me.nghlong3004.vqc.api.rubric.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.rubric.entity.Rubric;
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface RubricRepository extends JpaRepository<Rubric, Long> {

  Page<Rubric> findByProject(Project project, Pageable pageable);

  Page<Rubric> findByProjectAndStatus(Project project, RubricStatus status, Pageable pageable);

  Optional<Rubric> findByPublicIdAndCreatedBy(UUID publicId, User createdBy);

  Page<Rubric> findByCreatedBy(User createdBy, Pageable pageable);

  Page<Rubric> findByCreatedByAndStatus(User createdBy, RubricStatus status, Pageable pageable);

  Page<Rubric> findByIsTemplateTrueAndStatus(RubricStatus status, Pageable pageable);
}
