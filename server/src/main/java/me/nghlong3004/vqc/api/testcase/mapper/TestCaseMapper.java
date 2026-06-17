package me.nghlong3004.vqc.api.testcase.mapper;

import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.response.TestCaseResponse;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Component
public class TestCaseMapper {

  /**
   * Maps an internal {@link TestCase} entity to a public API response.
   *
   * @param testCase internal {@link TestCase} entity
   * @return public {@link TestCaseResponse}
   */
  public TestCaseResponse toResponse(TestCase testCase) {
    return new TestCaseResponse(
        testCase.getPublicId(),
        testCase.getDataset().getPublicId(),
        testCase.getExternalId(),
        testCase.getQuestion(),
        testCase.getTurns(),
        testCase.getPrecondition(),
        testCase.getGroundTruth(),
        testCase.getMetadata(),
        testCase.getStatus(),
        testCase.getSortOrder(),
        testCase.getCreatedAt(),
        testCase.getUpdatedAt());
  }
}
