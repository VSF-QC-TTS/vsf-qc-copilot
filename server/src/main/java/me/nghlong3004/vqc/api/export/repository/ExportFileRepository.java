package me.nghlong3004.vqc.api.export.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.export.entity.ExportFile;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface ExportFileRepository extends JpaRepository<ExportFile, Long> {

  Optional<ExportFile> findByPublicIdAndCreatedBy(UUID publicId, User createdBy);
}
