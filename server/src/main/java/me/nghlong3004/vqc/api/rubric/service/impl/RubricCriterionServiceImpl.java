package me.nghlong3004.vqc.api.rubric.service.impl;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;
import me.nghlong3004.vqc.api.rubric.mapper.RubricMapper;
import me.nghlong3004.vqc.api.rubric.repository.RubricCriterionRepository;
import me.nghlong3004.vqc.api.rubric.repository.RubricVersionRepository;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricCriterionRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricCriterionPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricCriterionResponse;
import me.nghlong3004.vqc.api.rubric.service.RubricCriterionService;
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
public class RubricCriterionServiceImpl implements RubricCriterionService {

  private final RubricCriterionRepository rubricCriterionRepository;
  private final RubricVersionRepository rubricVersionRepository;
  private final UserRepository userRepository;
  private final RubricMapper rubricMapper;

  @Override
  @Transactional
  public RubricCriterionResponse createCriterion(
      UUID rubricVersionPublicId, CreateRubricCriterionRequest request, String username) {
    RubricVersion rubricVersion = findVersion(rubricVersionPublicId, username);
    ensureMutable(rubricVersion);
    String metricKey = request.metricKey().trim();
    if (rubricCriterionRepository.existsByRubricVersionAndMetricKey(rubricVersion, metricKey)) {
      throw new ResourceException(ErrorCode.RUBRIC_CRITERION_METRIC_KEY_CONFLICT);
    }
    RubricCriterion criterion =
        RubricCriterion.builder()
            .rubricVersion(rubricVersion)
            .name(request.name().trim())
            .description(trimToNull(request.description()))
            .weight(request.weight() != null ? request.weight() : 1)
            .passCondition(trimToNull(request.passCondition()))
            .failCondition(trimToNull(request.failCondition()))
            .judgeInstruction(request.judgeInstruction().trim())
            .metricKey(metricKey)
            .critical(Boolean.TRUE.equals(request.isCritical()))
            .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
            .build();
    RubricCriterion saved = rubricCriterionRepository.save(criterion);
    log.info(
        "Created rubric criterion {} under version {}",
        saved.getPublicId(),
        rubricVersion.getPublicId());
    return rubricMapper.toCriterionResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public RubricCriterionPageResponse listCriteria(
      UUID rubricVersionPublicId, Pageable pageable, String username) {
    RubricVersion rubricVersion = findVersion(rubricVersionPublicId, username);
    Page<RubricCriterion> criteria =
        rubricCriterionRepository.findByRubricVersion(rubricVersion, pageable);
    List<RubricCriterionResponse> items =
        criteria.getContent().stream().map(rubricMapper::toCriterionResponse).toList();
    return new RubricCriterionPageResponse(
        items,
        criteria.getNumber(),
        criteria.getSize(),
        criteria.getTotalElements(),
        criteria.getTotalPages());
  }

  @Override
  @Transactional
  public RubricCriterionResponse updateCriterion(
      UUID criterionPublicId, UpdateRubricCriterionRequest request, String username) {
    RubricCriterion criterion = findCriterion(criterionPublicId, username);
    ensureMutable(criterion.getRubricVersion());
    if (request.name() != null) {
      criterion.setName(request.name().trim());
    }
    if (request.description() != null) {
      criterion.setDescription(trimToNull(request.description()));
    }
    if (request.weight() != null) {
      criterion.setWeight(request.weight());
    }
    if (request.passCondition() != null) {
      criterion.setPassCondition(trimToNull(request.passCondition()));
    }
    if (request.failCondition() != null) {
      criterion.setFailCondition(trimToNull(request.failCondition()));
    }
    if (request.judgeInstruction() != null) {
      criterion.setJudgeInstruction(request.judgeInstruction().trim());
    }
    if (request.metricKey() != null) {
      String metricKey = request.metricKey().trim();
      if (rubricCriterionRepository.existsByRubricVersionAndMetricKeyAndPublicIdNot(
          criterion.getRubricVersion(), metricKey, criterion.getPublicId())) {
        throw new ResourceException(ErrorCode.RUBRIC_CRITERION_METRIC_KEY_CONFLICT);
      }
      criterion.setMetricKey(metricKey);
    }
    if (request.isCritical() != null) {
      criterion.setCritical(request.isCritical());
    }
    if (request.sortOrder() != null) {
      criterion.setSortOrder(request.sortOrder());
    }
    RubricCriterion saved = rubricCriterionRepository.save(criterion);
    log.info(
        "Updated rubric criterion {} under version {}",
        saved.getPublicId(),
        saved.getRubricVersion().getPublicId());
    return rubricMapper.toCriterionResponse(saved);
  }

  @Override
  @Transactional
  public void deleteCriterion(UUID criterionPublicId, String username) {
    RubricCriterion criterion = findCriterion(criterionPublicId, username);
    ensureMutable(criterion.getRubricVersion());
    rubricCriterionRepository.delete(criterion);
    log.info(
        "Deleted rubric criterion {} under version {}",
        criterion.getPublicId(),
        criterion.getRubricVersion().getPublicId());
  }

  private RubricVersion findVersion(UUID rubricVersionPublicId, String username) {
    User creator = findCreator(username);
    return rubricVersionRepository
        .findByPublicIdAndRubricCreatedBy(rubricVersionPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.RUBRIC_VERSION_NOT_FOUND));
  }

  private RubricCriterion findCriterion(UUID criterionPublicId, String username) {
    User creator = findCreator(username);
    return rubricCriterionRepository
        .findByPublicIdAndRubricVersionRubricCreatedBy(criterionPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.RUBRIC_CRITERION_NOT_FOUND));
  }

  private User findCreator(String username) {
    return userRepository
        .findByUsername(username.trim().toLowerCase())
        .orElseThrow(() -> new ResourceException(ErrorCode.USER_NOT_FOUND));
  }

  private void ensureMutable(RubricVersion rubricVersion) {
    if (rubricVersion.getRubric().getStatus() == RubricStatus.ARCHIVED) {
      throw new ResourceException(ErrorCode.RUBRIC_ARCHIVED);
    }
    if (rubricVersion.getStatus() != RubricVersionStatus.DRAFT) {
      throw new ResourceException(ErrorCode.RUBRIC_VERSION_IMMUTABLE);
    }
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
