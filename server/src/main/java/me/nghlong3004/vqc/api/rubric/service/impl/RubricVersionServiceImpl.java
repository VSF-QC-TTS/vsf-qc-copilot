package me.nghlong3004.vqc.api.rubric.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.rubric.entity.Rubric;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;
import me.nghlong3004.vqc.api.rubric.mapper.RubricMapper;
import me.nghlong3004.vqc.api.rubric.repository.RubricCriterionRepository;
import me.nghlong3004.vqc.api.rubric.repository.RubricRepository;
import me.nghlong3004.vqc.api.rubric.repository.RubricVersionRepository;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricVersionRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionListItemResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionResponse;
import me.nghlong3004.vqc.api.rubric.service.RubricVersionService;
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
public class RubricVersionServiceImpl implements RubricVersionService {


  private final RubricVersionRepository rubricVersionRepository;
  private final RubricRepository rubricRepository;
  private final RubricCriterionRepository rubricCriterionRepository;
  private final UserRepository userRepository;
  private final RubricMapper rubricMapper;

  @Override
  @Transactional
  public RubricVersionResponse createVersion(UUID rubricPublicId, String username) {
    return createVersion(rubricPublicId, null, username);
  }

  @Override
  @Transactional
  public RubricVersionResponse createVersion(
      UUID rubricPublicId, UUID sourceVersionPublicId, String username) {
    User creator = findCreator(username);
    Rubric rubric = findRubric(rubricPublicId, creator);
    ensureRubricActive(rubric);
    RubricVersion sourceVersion =
        sourceVersionPublicId == null ? null : findSourceVersion(sourceVersionPublicId, rubric, creator);
    int nextVersion =
        rubricVersionRepository
            .findTopByRubricOrderByVersionDesc(rubric)
            .map(version -> version.getVersion() + 1)
            .orElse(1);
    RubricVersion rubricVersion =
        RubricVersion.builder()
            .rubric(rubric)
            .version(nextVersion)
            .content(sourceVersion == null ? null : sourceVersion.getContent())
            .outputSchemaJson(sourceVersion == null ? null : sourceVersion.getOutputSchemaJson())
            .status(RubricVersionStatus.DRAFT)
            .createdBy(creator)
            .build();
    RubricVersion saved = rubricVersionRepository.save(rubricVersion);
    List<RubricCriterion> copiedCriteria =
        sourceVersion == null ? List.of() : copyCriteria(sourceVersion, saved);
    if (!copiedCriteria.isEmpty()) {
      rubricCriterionRepository.saveAll(copiedCriteria);
    }
    log.info(
        "Created rubric version {} for rubric {} by user {}",
        saved.getPublicId(),
        rubric.getPublicId(),
        creator.getPublicId());
    return copiedCriteria.isEmpty() ? toResponse(saved) : rubricMapper.toVersionResponse(saved, copiedCriteria);
  }

  @Override
  @Transactional(readOnly = true)
  public RubricVersionPageResponse listVersions(
      UUID rubricPublicId, RubricVersionStatus status, Pageable pageable, String username) {
    User creator = findCreator(username);
    Rubric rubric = findRubric(rubricPublicId, creator);
    Page<RubricVersion> versions =
        status == null
            ? rubricVersionRepository.findByRubric(rubric, pageable)
            : rubricVersionRepository.findByRubricAndStatus(rubric, status, pageable);
    List<RubricVersionListItemResponse> items =
        versions.getContent().stream()
            .map(
                version ->
                    rubricMapper.toVersionListItemResponse(
                        version, rubricCriterionRepository.countByRubricVersion(version)))
            .toList();
    return new RubricVersionPageResponse(
        items,
        versions.getNumber(),
        versions.getSize(),
        versions.getTotalElements(),
        versions.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public RubricVersionPageResponse listUserVersions(
      RubricVersionStatus status, Pageable pageable, String username) {
    User creator = findCreator(username);
    Page<RubricVersion> versions =
        status == null
            ? rubricVersionRepository.findByRubricCreatedBy(creator, pageable)
            : rubricVersionRepository.findByRubricCreatedByAndStatus(creator, status, pageable);
    List<RubricVersionListItemResponse> items =
        versions.getContent().stream()
            .map(
                version ->
                    rubricMapper.toVersionListItemResponse(
                        version, rubricCriterionRepository.countByRubricVersion(version)))
            .toList();
    return new RubricVersionPageResponse(
        items,
        versions.getNumber(),
        versions.getSize(),
        versions.getTotalElements(),
        versions.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public RubricVersionResponse getVersion(UUID rubricVersionPublicId, String username) {
    RubricVersion rubricVersion = findVersion(rubricVersionPublicId, username);
    return toResponse(rubricVersion);
  }

  @Override
  @Transactional
  public RubricVersionResponse updateVersion(
      UUID rubricVersionPublicId, UpdateRubricVersionRequest request, String username) {
    RubricVersion rubricVersion = findVersion(rubricVersionPublicId, username);
    ensureRubricActive(rubricVersion.getRubric());
    boolean hasContentPatch = request.content() != null || request.outputSchemaJson() != null;
    if (hasContentPatch) {
      if (rubricVersion.getStatus() != RubricVersionStatus.DRAFT) {
        throw new ResourceException(ErrorCode.RUBRIC_VERSION_IMMUTABLE);
      }
      if (request.content() != null) {
        rubricVersion.setContent(trimToNull(request.content()));
      }
      if (request.outputSchemaJson() != null) {
        rubricVersion.setOutputSchemaJson(trimToNull(request.outputSchemaJson()));
      }
    }
    RubricVersionStatus requestedStatus = request.status();
    if (requestedStatus == null) {
      RubricVersion saved = rubricVersionRepository.save(rubricVersion);
      return toResponse(saved);
    }
    if (rubricVersion.getStatus() == requestedStatus) {
      return toResponse(rubricVersion);
    }
    if (rubricVersion.getStatus() == RubricVersionStatus.ARCHIVED) {
      throw new ResourceException(ErrorCode.RUBRIC_VERSION_IMMUTABLE);
    }
    if (requestedStatus == RubricVersionStatus.DRAFT) {
      throw new ResourceException(ErrorCode.RUBRIC_VERSION_IMMUTABLE);
    }
    if (requestedStatus == RubricVersionStatus.PUBLISHED) {
      publish(rubricVersion);
    } else if (requestedStatus == RubricVersionStatus.ARCHIVED) {
      archive(rubricVersion);
    }
    RubricVersion saved = rubricVersionRepository.save(rubricVersion);
    log.info(
        "Updated rubric version {} for rubric {} to status {}",
        saved.getPublicId(),
        saved.getRubric().getPublicId(),
        saved.getStatus());
    return toResponse(saved);
  }

  private void publish(RubricVersion rubricVersion) {
    if (rubricVersion.getStatus() != RubricVersionStatus.DRAFT) {
      throw new ResourceException(ErrorCode.RUBRIC_VERSION_IMMUTABLE);
    }
    if (rubricVersion.getContent() == null || rubricVersion.getContent().isBlank()) {
      throw new ResourceException(ErrorCode.RUBRIC_VERSION_PUBLISH_INVALID);
    }
    List<RubricCriterion> criteria =
        rubricCriterionRepository.findByRubricVersionOrderBySortOrderAscIdAsc(rubricVersion);
    if (criteria.isEmpty()) {
      throw new ResourceException(ErrorCode.RUBRIC_VERSION_PUBLISH_INVALID);
    }
    rubricVersion.setStatus(RubricVersionStatus.PUBLISHED);
    rubricVersion.setPublishedAt(OffsetDateTime.now());
    Rubric rubric = rubricVersion.getRubric();
    rubric.setCurrentVersion(rubricVersion.getVersion());
    rubricRepository.save(rubric);
  }

  private void archive(RubricVersion rubricVersion) {
    rubricVersion.setStatus(RubricVersionStatus.ARCHIVED);
    Rubric rubric = rubricVersion.getRubric();
    if (rubricVersion.getVersion().equals(rubric.getCurrentVersion())) {
      Integer fallbackCurrentVersion =
          rubricVersionRepository
              .findTopByRubricAndStatusAndPublicIdNotOrderByVersionDesc(
                  rubric, RubricVersionStatus.PUBLISHED, rubricVersion.getPublicId())
              .map(RubricVersion::getVersion)
              .orElse(null);
      rubric.setCurrentVersion(fallbackCurrentVersion);
      rubricRepository.save(rubric);
    }
  }

  private RubricVersionResponse toResponse(RubricVersion rubricVersion) {
    return rubricMapper.toVersionResponse(
        rubricVersion,
        rubricCriterionRepository.findByRubricVersionOrderBySortOrderAscIdAsc(rubricVersion));
  }

  private RubricVersion findVersion(UUID rubricVersionPublicId, String username) {
    User creator = findCreator(username);
    return rubricVersionRepository
        .findByPublicIdAndRubricCreatedBy(rubricVersionPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.RUBRIC_VERSION_NOT_FOUND));
  }

  private Rubric findRubric(UUID rubricPublicId, User creator) {
    return rubricRepository
        .findByPublicIdAndCreatedBy(rubricPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.RUBRIC_NOT_FOUND));
  }

  private RubricVersion findSourceVersion(
      UUID sourceVersionPublicId, Rubric rubric, User creator) {
    RubricVersion source =
        rubricVersionRepository
            .findByPublicIdAndRubricCreatedBy(sourceVersionPublicId, creator)
            .orElseThrow(() -> new ResourceException(ErrorCode.RUBRIC_VERSION_NOT_FOUND));
    if (!source.getRubric().getPublicId().equals(rubric.getPublicId())) {
      throw new ResourceException(ErrorCode.RUBRIC_VERSION_NOT_FOUND);
    }
    return source;
  }

  private List<RubricCriterion> copyCriteria(
      RubricVersion sourceVersion, RubricVersion targetVersion) {
    return rubricCriterionRepository
        .findByRubricVersionOrderBySortOrderAscIdAsc(sourceVersion)
        .stream()
        .map(
            criterion ->
                RubricCriterion.builder()
                    .rubricVersion(targetVersion)
                    .name(criterion.getName())
                    .description(criterion.getDescription())
                    .weight(criterion.getWeight())
                    .passCondition(criterion.getPassCondition())
                    .failCondition(criterion.getFailCondition())
                    .judgeInstruction(criterion.getJudgeInstruction())
                    .metricKey(criterion.getMetricKey())
                    .critical(Boolean.TRUE.equals(criterion.getCritical()))
                    .sortOrder(criterion.getSortOrder())
                    .build())
        .toList();
  }

  private User findCreator(String username) {
    return userRepository
        .findByUsername(username.trim().toLowerCase())
        .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
  }

  private void ensureRubricActive(Rubric rubric) {
    if (rubric.getStatus() == RubricStatus.ARCHIVED) {
      throw new ResourceException(ErrorCode.RUBRIC_ARCHIVED);
    }
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
