package me.nghlong3004.vqc.api.judge.service.impl;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.common.crypto.AesGcmEncryptor;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.judge.entity.JudgeModel;
import me.nghlong3004.vqc.api.judge.mapper.JudgeModelMapper;
import me.nghlong3004.vqc.api.judge.repository.JudgeModelRepository;
import me.nghlong3004.vqc.api.judge.request.CreateJudgeModelRequest;
import me.nghlong3004.vqc.api.judge.request.UpdateJudgeModelRequest;
import me.nghlong3004.vqc.api.judge.response.JudgeModelPageResponse;
import me.nghlong3004.vqc.api.judge.response.JudgeModelResponse;
import me.nghlong3004.vqc.api.judge.service.JudgeModelService;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JudgeModelServiceImpl implements JudgeModelService {

  private final JudgeModelRepository judgeModelRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final AesGcmEncryptor aesGcmEncryptor;
  private final JudgeModelMapper judgeModelMapper;

  @Override
  @Transactional
  public JudgeModelResponse createJudgeModel(
      UUID projectPublicId, CreateJudgeModelRequest request, String username) {
    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);
    JudgeModel judgeModel =
        JudgeModel.builder()
            .project(project)
            .name(request.name().trim())
            .provider(request.provider())
            .modelName(request.modelName().trim())
            .baseUrl(trimToNull(request.baseUrl()))
            .configJson(trimToNull(request.configJson()))
            .active(request.active() == null || request.active())
            .createdBy(creator)
            .build();
    applyApiKey(judgeModel, request.apiKey());
    JudgeModel saved = judgeModelRepository.save(judgeModel);
    log.info(
        "Created judge model {} for project {} by user {}",
        saved.getPublicId(),
        project.getPublicId(),
        creator.getPublicId());
    return judgeModelMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public JudgeModelPageResponse listJudgeModels(
      UUID projectPublicId, Boolean active, Pageable pageable, String username) {
    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);
    Page<JudgeModel> page =
        active == null
            ? judgeModelRepository.findByProject(project, pageable)
            : judgeModelRepository.findByProjectAndActive(project, active, pageable);
    List<JudgeModelResponse> items =
        page.getContent().stream().map(judgeModelMapper::toResponse).toList();
    return new JudgeModelPageResponse(
        items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  @Override
  @Transactional
  public JudgeModelResponse updateJudgeModel(
      UUID judgeModelPublicId, UpdateJudgeModelRequest request, String username) {
    JudgeModel judgeModel = findJudgeModel(judgeModelPublicId, username);
    if (request.name() != null) {
      judgeModel.setName(request.name().trim());
    }
    if (request.provider() != null) {
      judgeModel.setProvider(request.provider());
    }
    if (request.modelName() != null) {
      judgeModel.setModelName(request.modelName().trim());
    }
    if (request.baseUrl() != null) {
      judgeModel.setBaseUrl(trimToNull(request.baseUrl()));
    }
    if (request.configJson() != null) {
      judgeModel.setConfigJson(trimToNull(request.configJson()));
    }
    if (request.active() != null) {
      judgeModel.setActive(request.active());
    }
    if (request.apiKey() != null) {
      applyApiKey(judgeModel, request.apiKey());
    }
    return judgeModelMapper.toResponse(judgeModelRepository.save(judgeModel));
  }

  @Override
  @Transactional(readOnly = true)
  public JudgeModelResponse testConnection(UUID judgeModelPublicId, String username) {
    JudgeModel judgeModel = findJudgeModel(judgeModelPublicId, username);
    if (!Boolean.TRUE.equals(judgeModel.getActive())) {
      throw new ResourceException(ErrorCode.JUDGE_MODEL_INACTIVE);
    }
    return judgeModelMapper.toResponse(judgeModel);
  }

  private JudgeModel findJudgeModel(UUID judgeModelPublicId, String username) {
    User creator = findCreator(username);
    return judgeModelRepository
        .findByPublicIdAndCreatedBy(judgeModelPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.JUDGE_MODEL_NOT_FOUND));
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

  private void applyApiKey(JudgeModel judgeModel, String rawApiKey) {
    if (rawApiKey == null || rawApiKey.isBlank()) {
      return;
    }
    String trimmed = rawApiKey.trim();
    judgeModel.setEncryptedApiKey(aesGcmEncryptor.encrypt(trimmed));
    judgeModel.setApiKeyMasked(mask(trimmed));
  }

  private String mask(String value) {
    if (value.length() <= 4) {
      return "****";
    }
    return "****" + value.substring(value.length() - 4);
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  @Override
  @Transactional
  public void deleteJudgeModel(UUID judgeModelPublicId, String username) {
    JudgeModel judgeModel = findJudgeModel(judgeModelPublicId, username);
    judgeModelRepository.delete(judgeModel);
    log.info("Deleted judge model {} by user {}", judgeModelPublicId, username);
  }
}
