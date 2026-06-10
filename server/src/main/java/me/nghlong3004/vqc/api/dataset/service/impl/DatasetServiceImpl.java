package me.nghlong3004.vqc.api.dataset.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.mapper.DatasetMapper;
import me.nghlong3004.vqc.api.dataset.repository.DatasetRepository;
import me.nghlong3004.vqc.api.dataset.request.CreateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.response.DatasetResponse;
import me.nghlong3004.vqc.api.dataset.service.DatasetService;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import me.nghlong3004.vqc.api.requirement.repository.BusinessRequirementRepository;
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
@Slf4j
public class DatasetServiceImpl implements DatasetService {

  private final DatasetRepository datasetRepository;
  private final ProjectRepository projectRepository;
  private final BusinessRequirementRepository businessRequirementRepository;
  private final UserRepository userRepository;
  private final DatasetMapper datasetMapper;

  @Override
  @Transactional
  public DatasetResponse createDataset(
      UUID projectPublicId, CreateDatasetRequest request, String username) {
    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);
    BusinessRequirement requirement = findRequirement(request.requirementPublicId(), creator, project);
    Dataset dataset =
        Dataset.builder()
            .project(project)
            .requirement(requirement)
            .name(request.name().trim())
            .description(trimToNull(request.description()))
            .version(1)
            .sourceType(request.sourceType())
            .status(DatasetStatus.DRAFT)
            .createdBy(creator)
            .build();
    Dataset saved = datasetRepository.save(dataset);
    log.info(
        "Created dataset {} under project {} by user {}",
        saved.getPublicId(),
        project.getPublicId(),
        creator.getPublicId());
    return datasetMapper.toResponse(saved, 0);
  }

  private BusinessRequirement findRequirement(
      UUID requirementPublicId, User creator, Project project) {
    if (requirementPublicId == null) {
      return null;
    }
    BusinessRequirement requirement =
        businessRequirementRepository
            .findByPublicIdAndCreatedBy(requirementPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.REQUIREMENT_NOT_FOUND));
    if (!project.getPublicId().equals(requirement.getProject().getPublicId())) {
      throw new ResourceException(ErrorCode.REQUIREMENT_NOT_FOUND);
    }
    return requirement;
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

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
