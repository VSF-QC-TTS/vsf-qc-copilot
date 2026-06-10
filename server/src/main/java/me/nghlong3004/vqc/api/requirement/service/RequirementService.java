package me.nghlong3004.vqc.api.requirement.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import me.nghlong3004.vqc.api.requirement.enums.RequirementStatus;
import me.nghlong3004.vqc.api.requirement.request.CreateRequirementRequest;
import me.nghlong3004.vqc.api.requirement.response.RequirementPageResponse;
import me.nghlong3004.vqc.api.requirement.response.RequirementResponse;
import org.springframework.data.domain.Pageable;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface RequirementService {

  /**
   * Creates a new {@link BusinessRequirement} under a project owned by the authenticated user.
   *
   * @param projectPublicId public project identifier
   * @param request create requirement payload
   * @param username authenticated username/email
   * @return created public {@link RequirementResponse}
   */
  RequirementResponse createRequirement(
      UUID projectPublicId, CreateRequirementRequest request, String username);

  /**
   * Lists {@link BusinessRequirement} entities under a project owned by the authenticated user.
   *
   * @param projectPublicId public project identifier
   * @param status optional requirement status filter
   * @param pageable page and sort request
   * @param username authenticated username/email
   * @return paginated public {@link RequirementPageResponse}
   */
  RequirementPageResponse listRequirements(
      UUID projectPublicId, RequirementStatus status, Pageable pageable, String username);

  /**
   * Returns a {@link BusinessRequirement} owned by the authenticated user.
   *
   * @param requirementPublicId public requirement identifier
   * @param username authenticated username/email
   * @return public {@link RequirementResponse}
   */
  RequirementResponse getRequirement(UUID requirementPublicId, String username);
}
