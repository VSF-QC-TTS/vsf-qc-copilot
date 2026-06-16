package me.nghlong3004.vqc.api.redteam.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.redteam.entity.RedTeamRun;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
public interface RedTeamRunRepository extends JpaRepository<RedTeamRun, Long> {

  @EntityGraph(attributePaths = {"project", "targetApiConnector", "judgeModel", "job"})
  Optional<RedTeamRun> findByPublicIdAndCreatedBy(UUID publicId, User createdBy);

  @EntityGraph(attributePaths = {"project", "targetApiConnector", "judgeModel", "job", "createdBy"})
  Optional<RedTeamRun> findWithWorkerContextById(Long id);

  Page<RedTeamRun> findByProjectIdAndCreatedBy(Long projectId, User createdBy, Pageable pageable);
}
