package me.nghlong3004.vqc.api.job.worker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.config.PromptfooProperties;
import me.nghlong3004.vqc.api.config.WorkerProperties;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationResult;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.evaluation.enums.EvaluationRunStatus;
import me.nghlong3004.vqc.api.evaluation.executor.CliPromptfooExecutor;
import me.nghlong3004.vqc.api.evaluation.handler.EvaluationJobHandler;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooCommandExecutor;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooConfigGenerator;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooResultParser;
import me.nghlong3004.vqc.api.evaluation.promptfoo.PromptfooRunDirectoryResolver;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationResultRepository;
import me.nghlong3004.vqc.api.evaluation.repository.EvaluationRunRepository;
import me.nghlong3004.vqc.api.export.handler.ExportJobHandler;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.entity.JobEvent;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.repository.JobEventRepository;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import me.nghlong3004.vqc.api.targetconnector.service.ConnectorSecretService;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;
import me.nghlong3004.vqc.api.testcase.repository.TestCaseRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.domain.PageImpl;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
class PromptfooWorkerSmokeTest {

  @Test
  @EnabledIfSystemProperty(named = "vqc.promptfoo.smoke", matches = "true")
  void workerProcessesEvaluationJobWithRealPromptfooCli() throws Exception {
    Path smokeRoot = Path.of("target/promptfoo-smoke").toAbsolutePath().normalize();
    Files.createDirectories(smokeRoot);
    HttpServer targetServer = targetServer();
    try {
      int port = targetServer.getAddress().getPort();
      Fixture fixture = fixture("http://127.0.0.1:" + port + "/chat");
      JobWorker worker =
          new JobWorker(
              null,
              new WorkerProperties(),
              jobRepository(fixture.job),
              fixture.handler(promptfooExecutor(smokeRoot)),
              new ExportJobHandler(null, null, null, null, null));

      worker.processMessage(fixture.job.getPublicId().toString());

      assertThat(fixture.job.getStatus()).isEqualTo(JobStatus.COMPLETED);
      assertThat(fixture.run.getStatus()).isEqualTo(EvaluationRunStatus.COMPLETED);
      assertThat(fixture.savedResults).hasSize(1);
      assertThat(fixture.savedResults.getFirst().getActualAnswer()).contains("Answer");
      assertThat(
              smokeRoot
                  .resolve("runs")
                  .resolve(fixture.run.getPublicId().toString())
                  .resolve("results.json"))
          .exists();
    } finally {
      targetServer.stop(0);
    }
  }

  private HttpServer targetServer() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/chat",
        exchange -> {
          byte[] response = "{\"answer\":\"Answer\"}".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    return server;
  }

  private CliPromptfooExecutor promptfooExecutor(Path tempDir) {
    PromptfooProperties properties = new PromptfooProperties();
    properties.setBinaryPath("../tooling/promptfoo-runner/node_modules/.bin/promptfoo");
    properties.setWorkDir(tempDir.resolve("runs").toString());
    properties.setMaxConcurrency(1);
    properties.setMaxEvalTimeMs(120000);
    properties.setPerTestTimeoutMs(30000);
    var objectMapper = JsonMapper.builder().findAndAddModules().build();
    return new CliPromptfooExecutor(
        new PromptfooRunDirectoryResolver(properties),
        new PromptfooConfigGenerator(objectMapper),
        new PromptfooCommandExecutor(properties),
        new PromptfooResultParser(objectMapper),
        new ConnectorSecretService() {
          @Override
          public void saveSecrets(TargetApiConnector c, java.util.Map<String, String> s) {}
          @Override
          public java.util.Map<String, String> decryptSecrets(TargetApiConnector c) { return Map.of(); }
        });
  }

  private Fixture fixture(String targetUrl) {
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
            .name("Local smoke target")
            .method(HttpMethodType.POST)
            .url(targetUrl)
            .bodyTemplate(Map.of("question", "{{question}}"))
            .responseSelector("$.answer")
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
            .progressTotal(1)
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
            .totalCases(1)
            .createdBy(creator)
            .build();
    TestCase testCase =
        TestCase.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .dataset(dataset)
            .question("Question")
            .groundTruth("Answer")
            .status(TestCaseStatus.ACTIVE)
            .build();
    return new Fixture(job, run, List.of(testCase));
  }

  private JobRepository jobRepository(Job job) {
    return proxy(
        JobRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicId".equals(method.getName())) return Optional.of(job);
          if ("save".equals(method.getName())) return args[0];
          throw new UnsupportedOperationException(method.getName());
        });
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

    private EvaluationJobHandler handler(CliPromptfooExecutor executor) {
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
                if ("findById".equals(method.getName())) return Optional.of(runRef.get());
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
          JsonMapper.builder().findAndAddModules().build());
    }
  }
}
