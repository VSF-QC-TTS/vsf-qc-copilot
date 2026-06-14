package me.nghlong3004.vqc.api.dataset.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.dataset.enums.DatasetStatus;
import me.nghlong3004.vqc.api.dataset.repository.DatasetRepository;
import me.nghlong3004.vqc.api.dataset.request.GenerateDatasetRequest;
import me.nghlong3004.vqc.api.dataset.response.GenerateDatasetResponse;
import me.nghlong3004.vqc.api.dataset.service.DatasetGenerationService;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.job.service.JobQueuePublisher;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetGenerationServiceImpl implements DatasetGenerationService {

  private static final int MAX_TEST_CASES = 100;

  private final DatasetRepository datasetRepository;
  private final TestCaseRepository testCaseRepository;
  private final JobRepository jobRepository;
  private final UserRepository userRepository;
  private final JobQueuePublisher jobQueuePublisher;

  @Override
  @Transactional
  public GenerateDatasetResponse generateTestCases(
      UUID datasetPublicId, GenerateDatasetRequest request, String username) {

    User creator = findCreator(username);
    Dataset dataset = findDataset(datasetPublicId, creator);

    if (dataset.getStatus() != DatasetStatus.DRAFT) {
      throw new ResourceException(ErrorCode.DATASET_ARCHIVED);
    }

    long existingCount =
        testCaseRepository.countByDatasetAndStatus(dataset, TestCaseStatus.ACTIVE);
    if (existingCount + request.count() > MAX_TEST_CASES) {
      throw new ResourceException(ErrorCode.IMPORT_TOO_MANY_ROWS);
    }

    // Store the generation prompt for the job handler to use
    dataset.setGenerationPrompt(request.prompt());
    datasetRepository.save(dataset);

    Job job =
        Job.builder()
            .jobType(JobType.DATASET_GENERATION)
            .status(JobStatus.PENDING)
            .resourceType(ResourceType.DATASET)
            .resourceId(dataset.getId())
            .project(dataset.getProject())
            .createdBy(creator)
            .progressTotal(request.count())
            .build();
    Job savedJob = jobRepository.save(job);

    jobQueuePublisher.publish(savedJob.getPublicId().toString());

    log.info(
        "Created generation job {} for dataset {} with count {} by user {}",
        savedJob.getPublicId(),
        dataset.getPublicId(),
        request.count(),
        creator.getPublicId());

    return new GenerateDatasetResponse(
        dataset.getPublicId(),
        savedJob.getPublicId(),
        savedJob.getStatus().name(),
        "Dataset generation queued successfully.");
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
}
