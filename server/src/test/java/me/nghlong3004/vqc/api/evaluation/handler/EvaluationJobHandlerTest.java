package me.nghlong3004.vqc.api.evaluation.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.enums.EvaluationRunStatus;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;
import me.nghlong3004.vqc.api.evaluation.executor.PromptfooExecutor;
import me.nghlong3004.vqc.api.evaluation.executor.PromptfooResult;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationResultRepository;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationRunRepository;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.entity.JobEvent;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.repository.JobEventRepository;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.rubric.repository.RubricCriterionRepository;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
class EvaluationJobHandlerTest {

  @Test
  void handleCompletesJobRunAndWritesResults() {
    Fixture fixture = fixture();
    PromptfooExecutor executor =
        (run, testCases) ->
            testCases.stream()
                .map(
                    testCase ->
                        new PromptfooResult(
                            testCase.getId(),
                            "actual " + testCase.getId(),
                            BigDecimal.valueOf(0.9),
                            JudgeStatus.PASS,
                            "ok",
                            120,
                            null,
                            "{\"mode\":\"test\"}",
                            null))
                .toList();
    EvaluationJobHandler handler = fixture.handler(executor);

    handler.handle(fixture.job.getPublicId());

    assertThat(fixture.job.getStatus()).isEqualTo(JobStatus.COMPLETED);
    assertThat(fixture.job.getProgressCurrent()).isEqualTo(2);
    assertThat(fixture.run.getStatus()).isEqualTo(EvaluationRunStatus.COMPLETED);
    assertThat(fixture.run.getPassedCases()).isEqualTo(2);
    assertThat(fixture.run.getPassRate()).isEqualByComparingTo("1.0000");
    assertThat(fixture.savedResults).hasSize(2);
    assertThat(fixture.savedEvents).extracting(JobEvent::getEventType)
        .contains("RUNNING", "CASE_COMPLETED", "COMPLETED");
  }

  @Test
  void handleMarksJobAndRunFailedWhenExecutorThrows() {
    Fixture fixture = fixture();
    EvaluationJobHandler handler =
        fixture.handler(
            (run, testCases) -> {
              throw new IllegalStateException("executor failed");
            });

    handler.handle(fixture.job.getPublicId());

    assertThat(fixture.job.getStatus()).isEqualTo(JobStatus.FAILED);
    assertThat(fixture.job.getErrorMessage()).isEqualTo("executor failed");
    assertThat(fixture.run.getStatus()).isEqualTo(EvaluationRunStatus.FAILED);
    assertThat(fixture.savedResults).isEmpty();
    assertThat(fixture.savedEvents).extracting(JobEvent::getEventType).contains("FAILED");
  }

  @Test
  void handleWritesErrorResultWhenExecutorOmitsCase() {
    Fixture fixture = fixture();
    EvaluationJobHandler handler =
        fixture.handler(
            (run, testCases) ->
                List.of(
                    new PromptfooResult(
                        testCases.getFirst().getId(),
                        "actual",
                        BigDecimal.ONE,
                        JudgeStatus.PASS,
                        "ok",
                        100,
                        null,
                        "{}",
                        null)));

    handler.handle(fixture.job.getPublicId());

    assertThat(fixture.job.getStatus()).isEqualTo(JobStatus.COMPLETED);
    assertThat(fixture.savedResults).hasSize(2);
    assertThat(fixture.savedResults).extracting(EvaluationResult::getJudgeStatus)
        .containsExactly(JudgeStatus.PASS, JudgeStatus.ERROR);
    assertThat(fixture.run.getErrorCases()).isEqualTo(1);
  }

  private Fixture fixture() {
    User creator =
        User.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .username("qc.demo@example.com")
            .displayName("QC Demo")
            .passwordHash("hash")
            .build();
    Project project =
        Project.builder().id(1L).publicId(UUID.randomUUID()).name("Project").createdBy(creator).build();
    Dataset dataset =
        Dataset.builder().id(1L).publicId(UUID.randomUUID()).name("Dataset").project(project).createdBy(creator).build();
    RubricVersion rubricVersion =
        RubricVersion.builder().id(1L).publicId(UUID.randomUUID()).createdBy(creator).build();
    TargetApiConnector connector =
        TargetApiConnector.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .project(project)
            .name("Connector")
            .url("https://example.test")
            .createdBy(creator)
            .build();
    Job job =
        Job.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .jobType(JobType.EVALUATION_RUN)
            .status(JobStatus.PENDING)
            .resourceType(ResourceType.EVALUATION_RUN)
            .resourceId(1L)
            .project(project)
            .createdBy(creator)
            .progressTotal(2)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
    EvaluationRun run =
        EvaluationRun.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .project(project)
            .dataset(dataset)
            .rubricVersion(rubricVersion)
            .targetApiConnector(connector)
            .job(job)
            .status(EvaluationRunStatus.PENDING)
            .totalCases(2)
            .createdBy(creator)
            .build();
    List<TestCase> testCases =
        List.of(testCase(1L, dataset, "Question 1"), testCase(2L, dataset, "Question 2"));
    return new Fixture(job, run, testCases);
  }

  private TestCase testCase(Long id, Dataset dataset, String question) {
    return TestCase.builder()
        .id(id)
        .publicId(UUID.randomUUID())
        .dataset(dataset)
        .question(question)
        .status(TestCaseStatus.ACTIVE)
        .build();
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  private class Fixture {
    private final Job job;
    private final EvaluationRun run;
    private final List<TestCase> testCases;
    private final List<EvaluationResult> savedResults = new ArrayList<>();
    private final List<JobEvent> savedEvents = new ArrayList<>();

    private Fixture(Job job, EvaluationRun run, List<TestCase> testCases) {
      this.job = job;
      this.run = run;
      this.testCases = testCases;
    }

    private EvaluationJobHandler handler(PromptfooExecutor executor) {
      AtomicReference<Job> jobRef = new AtomicReference<>(job);
      AtomicReference<EvaluationRun> runRef = new AtomicReference<>(run);
      return new EvaluationJobHandler(
          proxy(
              JobRepository.class,
              (proxy, method, args) -> {
                if ("findByPublicId".equals(method.getName())) return Optional.of(jobRef.get());
                if ("save".equals(method.getName())) {
                  jobRef.set((Job) args[0]);
                  return args[0];
                }
                throw new UnsupportedOperationException(method.getName());
              }),
          proxy(
              JobEventRepository.class,
              (proxy, method, args) -> {
                if ("save".equals(method.getName())) {
                  savedEvents.add((JobEvent) args[0]);
                  return args[0];
                }
                throw new UnsupportedOperationException(method.getName());
              }),
          proxy(
              EvaluationRunRepository.class,
              (proxy, method, args) -> {
                if ("findWithWorkerContextById".equals(method.getName())) return Optional.of(runRef.get());
                if ("save".equals(method.getName())) {
                  runRef.set((EvaluationRun) args[0]);
                  return args[0];
                }
                throw new UnsupportedOperationException(method.getName());
              }),
          proxy(
              EvaluationResultRepository.class,
              (proxy, method, args) -> {
                if ("save".equals(method.getName())) {
                  savedResults.add((EvaluationResult) args[0]);
                  return args[0];
                }
                if ("findByEvaluationRunId".equals(method.getName())) {
                  return new PageImpl<>(savedResults);
                }
                if ("countByEvaluationRunIdAndJudgeStatus".equals(method.getName())) {
                  JudgeStatus status = (JudgeStatus) args[1];
                  return savedResults.stream().filter(r -> r.getJudgeStatus() == status).count();
                }
                throw new UnsupportedOperationException(method.getName());
              }),
          proxy(
              TestCaseRepository.class,
              (proxy, method, args) -> {
                if ("findByDatasetAndStatusOrderBySortOrderAscIdAsc".equals(method.getName())) {
                  return testCases;
                }
                throw new UnsupportedOperationException(method.getName());
              }),
          executor,
          JsonMapper.builder().findAndAddModules().build(),
          new CriteriaScoreCalculator(JsonMapper.builder().build()),
          proxy(
              RubricCriterionRepository.class,
              (proxy, method, args) -> {
                if (method.getReturnType() == List.class) return List.of();
                if (method.getReturnType() == long.class) return 0L;
                if (method.getReturnType() == boolean.class) return false;
                return null;
              }));
    }
  }
}
