package me.nghlong3004.vqc.api.testcase.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.response.TestCaseResponse;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class TestCaseMapperTest {

  private final TestCaseMapper testCaseMapper = new TestCaseMapper();

  @Test
  void toResponseMapsPublicTestCaseFields() {
    Dataset dataset = new Dataset();
    dataset.setPublicId(UUID.fromString("0f6d90c2-7410-4db2-86be-8adfd3140f63"));
    TestCase testCase = new TestCase();
    testCase.setPublicId(UUID.fromString("b4788db3-6cf3-47df-8ae1-4c73dbb7d0a8"));
    testCase.setDataset(dataset);
    testCase.setExternalId("HEALTH_001");
    testCase.setQuestion("How many steps did I walk today?");
    testCase.setPrecondition(Map.of("steps", 8200));
    testCase.setGroundTruth("The user walked 8,200 steps today.");
    testCase.setMetadata(Map.of("userId", "demo-user-1"));
    testCase.setStatus(TestCaseStatus.ACTIVE);
    testCase.setSortOrder(1);
    testCase.setCreatedAt(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    testCase.setUpdatedAt(OffsetDateTime.parse("2026-06-08T10:35:00Z"));

    TestCaseResponse response = testCaseMapper.toResponse(testCase);

    assertThat(response.publicId()).isEqualTo(testCase.getPublicId());
    assertThat(response.datasetPublicId()).isEqualTo(dataset.getPublicId());
    assertThat(response.externalId()).isEqualTo("HEALTH_001");
    assertThat(response.question()).isEqualTo("How many steps did I walk today?");
    assertThat(response.precondition()).containsEntry("steps", 8200);
    assertThat(response.groundTruth()).isEqualTo("The user walked 8,200 steps today.");
    assertThat(response.metadata()).containsEntry("userId", "demo-user-1");
    assertThat(response.status()).isEqualTo(TestCaseStatus.ACTIVE);
    assertThat(response.sortOrder()).isEqualTo(1);
    assertThat(response.createdAt()).isEqualTo(OffsetDateTime.parse("2026-06-08T10:30:00Z"));
    assertThat(response.updatedAt()).isEqualTo(OffsetDateTime.parse("2026-06-08T10:35:00Z"));
  }
}
