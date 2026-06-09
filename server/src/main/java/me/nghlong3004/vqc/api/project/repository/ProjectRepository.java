package me.nghlong3004.vqc.api.project.repository;

import me.nghlong3004.vqc.api.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {}
