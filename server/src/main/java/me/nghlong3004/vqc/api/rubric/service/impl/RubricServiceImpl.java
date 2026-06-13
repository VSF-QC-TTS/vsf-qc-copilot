package me.nghlong3004.vqc.api.rubric.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.rubric.entity.Rubric;
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.rubric.mapper.RubricMapper;
import me.nghlong3004.vqc.api.rubric.repository.RubricRepository;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricRequest;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricListItemResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricResponse;
import me.nghlong3004.vqc.api.rubric.service.RubricService;
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
public class RubricServiceImpl implements RubricService {

  private final RubricRepository rubricRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final RubricMapper rubricMapper;

  @Override
  @Transactional
  public RubricResponse createRubric(
      UUID projectPublicId, CreateRubricRequest request, String username) {
    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);
    Rubric rubric =
        Rubric.builder()
            .project(project)
            .name(request.name().trim())
            .description(trimToNull(request.description()))
            .status(RubricStatus.ACTIVE)
            .createdBy(creator)
            .build();
    Rubric saved = rubricRepository.save(rubric);
    log.info(
        "Created rubric {} under project {} by user {}",
        saved.getPublicId(),
        project.getPublicId(),
        creator.getPublicId());
    return rubricMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public RubricPageResponse listRubrics(
      UUID projectPublicId, RubricStatus status, Pageable pageable, String username) {
    User creator = findCreator(username);
    Project project = findProject(projectPublicId, creator);
    Page<Rubric> rubrics =
        status == null
            ? rubricRepository.findByProject(project, pageable)
            : rubricRepository.findByProjectAndStatus(project, status, pageable);
    List<RubricListItemResponse> items =
        rubrics.getContent().stream().map(rubricMapper::toListItemResponse).toList();
    return new RubricPageResponse(
        items,
        rubrics.getNumber(),
        rubrics.getSize(),
        rubrics.getTotalElements(),
        rubrics.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public RubricResponse getRubric(UUID rubricPublicId, String username) {
    return rubricMapper.toResponse(findRubric(rubricPublicId, username));
  }

  @Override
  @Transactional
  public RubricResponse updateRubric(
      UUID rubricPublicId, UpdateRubricRequest request, String username) {
    Rubric rubric = findRubric(rubricPublicId, username);
    ensureActive(rubric);
    if (request.name() != null) {
      rubric.setName(request.name().trim());
    }
    if (request.description() != null) {
      rubric.setDescription(trimToNull(request.description()));
    }
    Rubric saved = rubricRepository.save(rubric);
    log.info("Updated rubric {} by user {}", saved.getPublicId(), saved.getCreatedBy().getPublicId());
    return rubricMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void archiveRubric(UUID rubricPublicId, String username) {
    Rubric rubric = findRubric(rubricPublicId, username);
    rubric.setStatus(RubricStatus.ARCHIVED);
    if (rubric.getArchivedAt() == null) {
      rubric.setArchivedAt(OffsetDateTime.now());
    }
    rubricRepository.save(rubric);
    log.info("Archived rubric {} by user {}", rubric.getPublicId(), rubric.getCreatedBy().getPublicId());
  }

  @Override
  @Transactional(readOnly = true)
  public RubricPageResponse listMyRubrics(
      RubricStatus status, Pageable pageable, String username) {
    User creator = findCreator(username);
    Page<Rubric> rubrics =
        status == null
            ? rubricRepository.findByCreatedBy(creator, pageable)
            : rubricRepository.findByCreatedByAndStatus(creator, status, pageable);
    List<RubricListItemResponse> items =
        rubrics.getContent().stream().map(rubricMapper::toListItemResponse).toList();
    return new RubricPageResponse(
        items,
        rubrics.getNumber(),
        rubrics.getSize(),
        rubrics.getTotalElements(),
        rubrics.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public RubricPageResponse listTemplates(Pageable pageable, String username) {
    findCreator(username);
    Page<Rubric> rubrics =
        rubricRepository.findByIsTemplateTrueAndStatus(RubricStatus.ACTIVE, pageable);
    List<RubricListItemResponse> items =
        rubrics.getContent().stream().map(rubricMapper::toListItemResponse).toList();
    return new RubricPageResponse(
        items,
        rubrics.getNumber(),
        rubrics.getSize(),
        rubrics.getTotalElements(),
        rubrics.getTotalPages());
  }

  @Override
  @Transactional
  public RubricResponse cloneRubric(UUID rubricPublicId, String username) {
    User creator = findCreator(username);
    Rubric source =
        rubricRepository
            .findByPublicIdAndCreatedBy(rubricPublicId, creator)
            .or(() -> rubricRepository.findAll().stream()
                .filter(r -> r.getPublicId().equals(rubricPublicId)
                    && Boolean.TRUE.equals(r.getIsTemplate()))
                .findFirst())
            .orElseThrow(() -> new ResourceException(ErrorCode.RUBRIC_NOT_FOUND));

    Rubric cloned =
        Rubric.builder()
            .project(source.getProject())
            .name(source.getName() + " (Copy)")
            .description(source.getDescription())
            .isTemplate(false)
            .status(RubricStatus.ACTIVE)
            .createdBy(creator)
            .build();
    Rubric saved = rubricRepository.save(cloned);
    log.info(
        "Cloned rubric {} from {} by user {}",
        saved.getPublicId(),
        source.getPublicId(),
        creator.getPublicId());
    return rubricMapper.toResponse(saved);
  }

  private Rubric findRubric(UUID rubricPublicId, String username) {
    User creator = findCreator(username);
    return rubricRepository
        .findByPublicIdAndCreatedBy(rubricPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.RUBRIC_NOT_FOUND));
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

  private void ensureActive(Rubric rubric) {
    if (rubric.getStatus() == RubricStatus.ARCHIVED) {
      throw new ResourceException(ErrorCode.RUBRIC_ARCHIVED);
    }
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
