package me.nghlong3004.vqc.api.requirement.mapper;

import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import me.nghlong3004.vqc.api.requirement.response.RequirementListItemResponse;
import me.nghlong3004.vqc.api.requirement.response.RequirementResponse;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Component
public class RequirementMapper {

  /**
   * Maps an internal {@link BusinessRequirement} entity to a public API response.
   *
   * @param requirement internal {@link BusinessRequirement} entity
   * @return public {@link RequirementResponse}
   */
  public RequirementResponse toResponse(BusinessRequirement requirement) {
    return new RequirementResponse(
        requirement.getPublicId(),
        requirement.getProject().getPublicId(),
        requirement.getContent(),
        requirement.getVersion(),
        requirement.getStatus(),
        requirement.getCreatedAt(),
        requirement.getUpdatedAt());
  }

  /**
   * Maps an internal {@link BusinessRequirement} entity to a public list item response.
   *
   * @param requirement internal {@link BusinessRequirement} entity
   * @return public {@link RequirementListItemResponse}
   */
  public RequirementListItemResponse toListItemResponse(BusinessRequirement requirement) {
    return new RequirementListItemResponse(
        requirement.getPublicId(),
        requirement.getProject().getPublicId(),
        requirement.getContent(),
        requirement.getVersion(),
        requirement.getStatus(),
        requirement.getCreatedAt());
  }
}
