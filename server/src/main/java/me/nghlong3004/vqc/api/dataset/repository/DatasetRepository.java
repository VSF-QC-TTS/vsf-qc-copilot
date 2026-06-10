package me.nghlong3004.vqc.api.dataset.repository;

import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface DatasetRepository extends JpaRepository<Dataset, Long> {}
