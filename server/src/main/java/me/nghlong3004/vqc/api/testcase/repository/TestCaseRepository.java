package me.nghlong3004.vqc.api.testcase.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

  /**
   * Counts test cases in a dataset by lifecycle status.
   *
   * @param dataset owner {@link Dataset}
   * @param status test case status
   * @return count of matching test cases
   */
  long countByDatasetAndStatus(Dataset dataset, TestCaseStatus status);

  /**
   * Finds test cases under a {@link Dataset}.
   *
   * @param dataset owner {@link Dataset}
   * @param pageable page and sort request
   * @return page of matching {@link TestCase} entities
   */
  Page<TestCase> findByDataset(Dataset dataset, Pageable pageable);

  /**
   * Finds test cases under a {@link Dataset} with a matching {@link TestCaseStatus}.
   *
   * @param dataset owner {@link Dataset}
   * @param status test case status filter
   * @param pageable page and sort request
   * @return page of matching {@link TestCase} entities
   */
  Page<TestCase> findByDatasetAndStatus(
      Dataset dataset, TestCaseStatus status, Pageable pageable);

  /**
   * Finds a test case by public id and dataset creator.
   *
   * @param publicId public test case identifier
   * @param createdBy dataset creator {@link User}
   * @return {@link Optional} containing the matching {@link TestCase} when present
   */
  Optional<TestCase> findByPublicIdAndDatasetCreatedBy(UUID publicId, User createdBy);
}
