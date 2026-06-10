package me.nghlong3004.vqc.api.requirement.repository;

import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface BusinessRequirementRepository extends JpaRepository<BusinessRequirement, Long> {}
