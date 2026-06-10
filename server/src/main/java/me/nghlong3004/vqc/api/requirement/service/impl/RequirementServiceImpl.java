package me.nghlong3004.vqc.api.requirement.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import me.nghlong3004.vqc.api.requirement.enums.RequirementStatus;
import me.nghlong3004.vqc.api.requirement.mapper.RequirementMapper;
import me.nghlong3004.vqc.api.requirement.repository.BusinessRequirementRepository;
import me.nghlong3004.vqc.api.requirement.request.CreateRequirementRequest;
import me.nghlong3004.vqc.api.requirement.response.RequirementResponse;
import me.nghlong3004.vqc.api.requirement.service.RequirementService;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Service
@RequiredArgsConstructor
public class RequirementServiceImpl implements RequirementService {

  private final BusinessRequirementRepository businessRequirementRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final RequirementMapper requirementMapper;

  @Override
  @Transactional
  public RequirementResponse createRequirement(
      UUID projectPublicId, CreateRequirementRequest request, String username) {
    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);
    BusinessRequirement requirement =
        BusinessRequirement.builder()
            .project(project)
            .content(request.content().trim())
            .version(1)
            .status(RequirementStatus.ACTIVE)
            .createdBy(creator)
            .build();
    BusinessRequirement saved = businessRequirementRepository.save(requirement);
    return requirementMapper.toResponse(saved);
  }

  private Project findProject(UUID projectPublicId, User creator) {
    return projectRepository
        .findByPublicIdAndCreatedBy(projectPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.PROJECT_NOT_FOUND));
  }

  private User findCreator(String username) {
    return userRepository
        .findByUsername(username.trim().toLowerCase())
        .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
  }
}
