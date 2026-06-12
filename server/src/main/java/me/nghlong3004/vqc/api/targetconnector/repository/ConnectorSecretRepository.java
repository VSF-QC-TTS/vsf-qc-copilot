package me.nghlong3004.vqc.api.targetconnector.repository;

import java.util.List;
import me.nghlong3004.vqc.api.targetconnector.entity.ConnectorSecret;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Repository
public interface ConnectorSecretRepository extends JpaRepository<ConnectorSecret, Long> {

  List<ConnectorSecret> findByConnector(TargetApiConnector connector);

  void deleteByConnector(TargetApiConnector connector);
}
