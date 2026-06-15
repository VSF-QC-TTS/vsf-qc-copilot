package me.nghlong3004.vqc.api.judge.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.judge.entity.JudgeModel;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
public interface JudgeModelRepository extends JpaRepository<JudgeModel, Long> {

  Page<JudgeModel> findByProject(Project project, Pageable pageable);

  Page<JudgeModel> findByProjectAndActive(Project project, Boolean active, Pageable pageable);

  Optional<JudgeModel> findByPublicIdAndCreatedBy(UUID publicId, User createdBy);
}
