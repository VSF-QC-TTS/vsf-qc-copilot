package me.nghlong3004.vqc.api.testcase.service.impl;

import java.util.UUID;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.repository.DatasetRepository;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.mapper.TestCaseMapper;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import me.nghlong3004.vqc.api.testcase.request.CreateTestCaseRequest;
import me.nghlong3004.vqc.api.testcase.response.TestCasePageResponse;
import me.nghlong3004.vqc.api.testcase.response.TestCaseResponse;
import me.nghlong3004.vqc.api.testcase.service.TestCaseService;
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
public class TestCaseServiceImpl implements TestCaseService {

  private final TestCaseRepository testCaseRepository;
  private final DatasetRepository datasetRepository;
  private final UserRepository userRepository;
  private final TestCaseMapper testCaseMapper;

  @Override
  @Transactional
  public TestCaseResponse createTestCase(
      UUID datasetPublicId, CreateTestCaseRequest request, String username) {
    User creator = findCreator(username);
    Dataset dataset = findDataset(datasetPublicId, creator);
    if (dataset.getStatus() == DatasetStatus.ARCHIVED) {
      throw new ResourceException(ErrorCode.DATASET_ARCHIVED);
    }
    TestCase testCase =
        TestCase.builder()
            .dataset(dataset)
            .externalId(trimToNull(request.externalId()))
            .question(request.question().trim())
            .precondition(request.precondition())
            .groundTruth(trimToNull(request.groundTruth()))
            .metadata(request.metadata())
            .status(request.status() == null ? TestCaseStatus.ACTIVE : request.status())
            .sortOrder(request.sortOrder())
            .build();
    TestCase saved = testCaseRepository.save(testCase);
    log.info(
        "Created test case {} under dataset {} by user {}",
        saved.getPublicId(),
        dataset.getPublicId(),
        creator.getPublicId());
    return testCaseMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public TestCasePageResponse listTestCases(
      UUID datasetPublicId, TestCaseStatus status, Pageable pageable, String username) {
    User creator = findCreator(username);
    Dataset dataset = findDataset(datasetPublicId, creator);
    Page<TestCase> testCases =
        status == null
            ? testCaseRepository.findByDataset(dataset, pageable)
            : testCaseRepository.findByDatasetAndStatus(dataset, status, pageable);
    List<TestCaseResponse> items =
        testCases.getContent().stream().map(testCaseMapper::toResponse).toList();
    log.info(
        "Listed test cases for dataset {} by user {} with status {} page {} size {}",
        dataset.getPublicId(),
        creator.getPublicId(),
        status,
        testCases.getNumber(),
        testCases.getSize());
    return new TestCasePageResponse(
        items,
        testCases.getNumber(),
        testCases.getSize(),
        testCases.getTotalElements(),
        testCases.getTotalPages());
  }

  private Dataset findDataset(UUID datasetPublicId, User creator) {
    return datasetRepository
        .findByPublicIdAndCreatedBy(datasetPublicId, creator)
        .orElseThrow(() -> new ResourceException(ErrorCode.DATASET_NOT_FOUND));
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
