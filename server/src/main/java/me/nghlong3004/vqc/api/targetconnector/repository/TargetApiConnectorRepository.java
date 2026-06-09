package me.nghlong3004.vqc.api.targetconnector.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface TargetApiConnectorRepository extends JpaRepository<TargetApiConnector, Long> {

  /**
   * Finds connectors under a {@link Project}.
   *
   * @param project owning project
   * @param pageable page and sort request
   * @return page of matching {@link TargetApiConnector} entities
   */
  Page<TargetApiConnector> findByProject(Project project, Pageable pageable);

  /**
   * Finds a connector by public id and creator.
   *
   * @param publicId public connector identifier
   * @param createdBy creator {@link User}
   * @return {@link Optional} containing the matching {@link TargetApiConnector} when present
   */
  Optional<TargetApiConnector> findByPublicIdAndCreatedBy(UUID publicId, User createdBy);
}
