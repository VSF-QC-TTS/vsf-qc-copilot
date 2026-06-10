package me.nghlong3004.vqc.api.testcase.repository;

import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
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
}
