package me.nghlong3004.vqc.api.job.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.exception.GlobalException;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.response.JobDetailResponse;
import me.nghlong3004.vqc.api.job.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@WebMvcTest(
    controllers = JobController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalException.class, JobControllerTest.MockBeans.class})
class JobControllerTest {

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetTestDoubles() {
    RecordingJobService.reset();
  }

  @Test
  void getJobReturnsJobDetail() throws Exception {
    RecordingJobService.jobDetailResponse =
        new JobDetailResponse(
            UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"),
            JobType.EVALUATION_RUN,
            JobStatus.COMPLETED,
            ResourceType.EVALUATION_RUN,
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            UUID.fromString("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"),
            10,
            10,
            null,
            OffsetDateTime.parse("2026-06-11T10:00:00Z"),
            OffsetDateTime.parse("2026-06-11T10:00:01Z"),
            OffsetDateTime.parse("2026-06-11T10:05:00Z"),
            OffsetDateTime.parse("2026-06-11T10:05:00Z"));

    mockMvc
        .perform(
            get("/api/v1/jobs/b2c3d4e5-f6a7-8901-bcde-f12345678901")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.publicId").value("b2c3d4e5-f6a7-8901-bcde-f12345678901"))
        .andExpect(jsonPath("$.jobType").value("EVALUATION_RUN"))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.resourceType").value("EVALUATION_RUN"))
        .andExpect(
            jsonPath("$.resourcePublicId")
                .value("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
        .andExpect(
            jsonPath("$.projectPublicId")
                .value("5a4edcc1-cd1e-44ef-a144-31f5f3d2f653"))
        .andExpect(jsonPath("$.progressCurrent").value(10))
        .andExpect(jsonPath("$.progressTotal").value(10))
        .andExpect(jsonPath("$.errorMessage").isEmpty());

    assertThat(RecordingJobService.jobPublicId)
        .isEqualTo(UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"));
    assertThat(RecordingJobService.username).isEqualTo("qc.demo@example.com");
  }

  @Test
  void getJobReturnsValidationProblemDetailsForInvalidId() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/jobs/not-a-uuid")
                .principal(new TestingAuthenticationToken("qc.demo@example.com", null)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.instance").value("/api/v1/jobs/not-a-uuid"));

    assertThat(RecordingJobService.jobPublicId).isNull();
  }

  @TestConfiguration
  static class MockBeans {

    @Bean
    JobService jobService() {
      return new RecordingJobService();
    }
  }

  static class RecordingJobService implements JobService {

    static UUID jobPublicId;
    static String username;
    static JobDetailResponse jobDetailResponse;

    static void reset() {
      jobPublicId = null;
      username = null;
      jobDetailResponse = null;
    }

    @Override
    public JobDetailResponse getJob(UUID jobPublicId, String username) {
      RecordingJobService.jobPublicId = jobPublicId;
      RecordingJobService.username = username;
      return jobDetailResponse;
    }
  }
}
