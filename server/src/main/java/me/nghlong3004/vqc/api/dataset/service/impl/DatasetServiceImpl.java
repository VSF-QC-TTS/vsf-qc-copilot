package me.nghlong3004.vqc.api.dataset.service.impl;

import java.util.UUID;
import java.util.List;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.mapper.DatasetMapper;
import me.nghlong3004.vqc.api.dataset.repository.DatasetRepository;
import me.nghlong3004.vqc.api.dataset.request.CreateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.request.UpdateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.response.DatasetListItemResponse;
import me.nghlong3004.vqc.api.dataset.response.DatasetPageResponse;
import me.nghlong3004.vqc.api.dataset.response.DatasetResponse;
import me.nghlong3004.vqc.api.dataset.service.DatasetService;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.requirement.entity.BusinessRequirement;
import me.nghlong3004.vqc.api.requirement.repository.BusinessRequirementRepository;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  private final TestCaseRepository testCaseRepository;
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

  @Override
  @Transactional(readOnly = true)
  public DatasetPageResponse listDatasets(
      UUID projectPublicId, DatasetStatus status, Pageable pageable, String username) {
    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);
    Page<Dataset> datasets =
        status == null
            ? datasetRepository.findByProject(project, pageable)
            : datasetRepository.findByProjectAndStatus(project, status, pageable);
    List<DatasetListItemResponse> items =
        datasets.getContent().stream()
            .map(dataset -> datasetMapper.toListItemResponse(dataset, countActiveCases(dataset)))
            .toList();
    log.info(
        "Listed datasets for project {} by user {} with status {} page {} size {}",
        project.getPublicId(),
        creator.getPublicId(),
        status,
        datasets.getNumber(),
        datasets.getSize());
    return new DatasetPageResponse(
        items,
        datasets.getNumber(),
        datasets.getSize(),
        datasets.getTotalElements(),
        datasets.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public DatasetResponse getDataset(UUID datasetPublicId, String username) {
    Dataset dataset = findDataset(datasetPublicId, username);
    log.info(
        "Loaded dataset {} under project {} by user {}",
        dataset.getPublicId(),
        dataset.getProject().getPublicId(),
        dataset.getCreatedBy().getPublicId());
    return datasetMapper.toResponse(dataset, countActiveCases(dataset));
  }

  @Override
  @Transactional
  public DatasetResponse updateDataset(
      UUID datasetPublicId, UpdateDatasetRequest request, String username) {
    User creator = findCreator(username);
    Dataset dataset = findDataset(datasetPublicId, creator);
    if (request.name() != null) {
      dataset.setName(request.name().trim());
    }
    if (request.description() != null) {
      dataset.setDescription(trimToNull(request.description()));
    }
    if (request.status() != null) {
      applyStatus(dataset, request.status(), creator);
    }
    Dataset saved = datasetRepository.save(dataset);
    long activeCases = countActiveCases(saved);
    log.info(
        "Updated dataset {} under project {} by user {} to status {}",
        saved.getPublicId(),
        saved.getProject().getPublicId(),
        creator.getPublicId(),
        saved.getStatus());
    return datasetMapper.toResponse(saved, activeCases);
  }

  private void applyStatus(Dataset dataset, DatasetStatus status, User creator) {
    if (status == DatasetStatus.APPROVED) {
      long activeCases = countActiveCases(dataset);
      if (activeCases < 1 || activeCases > 100) {
        throw new ResourceException(ErrorCode.DATASET_APPROVAL_INVALID);
      }
      dataset.setApprovedBy(creator);
      dataset.setApprovedAt(OffsetDateTime.now());
    } else {
      dataset.setApprovedBy(null);
      dataset.setApprovedAt(null);
    }
    dataset.setStatus(status);
  }

  private long countActiveCases(Dataset dataset) {
    return testCaseRepository.countByDatasetAndStatus(dataset, TestCaseStatus.ACTIVE);
  }

  private Dataset findDataset(UUID datasetPublicId, String username) {
    User creator = findCreator(username);
    return findDataset(datasetPublicId, creator);
  }

  private Dataset findDataset(UUID datasetPublicId, User creator) {
    return datasetRepository
        .findByPublicIdAndCreatedBy(datasetPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.DATASET_NOT_FOUND));
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
