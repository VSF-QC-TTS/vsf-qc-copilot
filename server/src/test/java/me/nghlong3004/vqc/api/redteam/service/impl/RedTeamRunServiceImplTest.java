package me.nghlong3004.vqc.api.redteam.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.judge.repository.JudgeModelRepository;
import me.nghlong3004.vqc.api.job.entity.Job;
import me.nghlong3004.vqc.api.job.enums.JobStatus;
import me.nghlong3004.vqc.api.job.enums.JobType;
import me.nghlong3004.vqc.api.job.enums.ResourceType;
import me.nghlong3004.vqc.api.job.repository.JobRepository;
import me.nghlong3004.vqc.api.job.service.JobQueuePublisher;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.redteam.entity.RedTeamRun;
import me.nghlong3004.vqc.api.redteam.enums.RedTeamRunStatus;
import me.nghlong3004.vqc.api.redteam.repository.RedTeamRunRepository;
import me.nghlong3004.vqc.api.redteam.request.CreateRedTeamRunRequest;
import me.nghlong3004.vqc.api.redteam.response.CreateRedTeamRunResponse;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import me.nghlong3004.vqc.api.targetconnector.repository.TargetApiConnectorRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
class RedTeamRunServiceImplTest {

  @Test
  void createRedTeamRunPersistsPendingRunAndQueuesJobWithDefaults() {
    User creator = user();
    Project project = project(creator);
    TargetApiConnector connector = connector(project, creator);
    AtomicReference<RedTeamRun> savedRun = new AtomicReference<>();
    AtomicReference<Job> savedJob = new AtomicReference<>();
    List<String> published = new ArrayList<>();
    RedTeamRunServiceImpl service =
        new RedTeamRunServiceImpl(
            redTeamRunRepository(savedRun),
            jobRepository(savedJob),
            projectRepository(project),
            connectorRepository(connector),
            ignoredRepository(JudgeModelRepository.class),
            userRepository(creator),
            queuePublisher(published),
            JsonMapper.builder().findAndAddModules().build());

    CreateRedTeamRunResponse response =
        service.createRedTeamRun(
            project.getPublicId(),
            new CreateRedTeamRunRequest(
                " Safety scan ",
                connector.getPublicId(),
                null,
                "Answer healthcare questions safely.",
                List.of(),
                null,
                null),
            creator.getUsername());

    assertThat(response.status()).isEqualTo(RedTeamRunStatus.PENDING.name());
    assertThat(response.runPublicId()).isEqualTo(savedRun.get().getPublicId());
    assertThat(response.jobPublicId()).isEqualTo(savedJob.get().getPublicId());
    assertThat(published).containsExactly(savedJob.get().getPublicId().toString());

    assertThat(savedRun.get().getName()).isEqualTo("Safety scan");
    assertThat(savedRun.get().getPluginsJson())
        .isEqualTo("[\"harmful:privacy\",\"prompt-extraction\",\"pii:direct\"]");
    assertThat(savedRun.get().getStrategiesJson()).isEqualTo("[\"basic\"]");
    assertThat(savedRun.get().getNumTests()).isEqualTo(1);
    assertThat(savedRun.get().getJob()).isSameAs(savedJob.get());

    assertThat(savedJob.get().getJobType()).isEqualTo(JobType.RED_TEAM_RUN);
    assertThat(savedJob.get().getStatus()).isEqualTo(JobStatus.PENDING);
    assertThat(savedJob.get().getResourceType()).isEqualTo(ResourceType.RED_TEAM_RUN);
    assertThat(savedJob.get().getResourceId()).isEqualTo(savedRun.get().getId());
  }

  private RedTeamRunRepository redTeamRunRepository(AtomicReference<RedTeamRun> saved) {
    return proxy(
        RedTeamRunRepository.class,
        (proxy, method, args) -> {
          if ("save".equals(method.getName())) {
            RedTeamRun run = (RedTeamRun) args[0];
            if (run.getId() == null) {
              run.setId(22L);
            }
            saved.set(run);
            return run;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private JobRepository jobRepository(AtomicReference<Job> saved) {
    return proxy(
        JobRepository.class,
        (proxy, method, args) -> {
          if ("save".equals(method.getName())) {
            Job job = (Job) args[0];
            if (job.getId() == null) {
              job.setId(33L);
            }
            saved.set(job);
            return job;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private ProjectRepository projectRepository(Project project) {
    return proxy(
        ProjectRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicIdAndCreatedBy".equals(method.getName())) {
            return Optional.of(project);
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private TargetApiConnectorRepository connectorRepository(TargetApiConnector connector) {
    return proxy(
        TargetApiConnectorRepository.class,
        (proxy, method, args) -> {
          if ("findByPublicIdAndCreatedBy".equals(method.getName())) {
            return Optional.of(connector);
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private UserRepository userRepository(User creator) {
    return proxy(
        UserRepository.class,
        (proxy, method, args) -> {
          if ("findByUsername".equals(method.getName())) {
            return Optional.of(creator);
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private JobQueuePublisher queuePublisher(List<String> published) {
    return new JobQueuePublisher(null, null) {
      @Override
      public void publish(String jobPublicId) {
        published.add(jobPublicId);
      }
    };
  }

  private User user() {
    return User.builder()
        .id(1L)
        .publicId(UUID.randomUUID())
        .username("qc.demo@example.com")
        .passwordHash("hash")
        .displayName("QC Demo")
        .build();
  }

  private Project project(User creator) {
    return Project.builder()
        .id(10L)
        .publicId(UUID.randomUUID())
        .name("Project")
        .createdBy(creator)
        .build();
  }

  private TargetApiConnector connector(Project project, User creator) {
    return TargetApiConnector.builder()
        .id(11L)
        .publicId(UUID.randomUUID())
        .project(project)
        .name("Target")
        .method(HttpMethodType.POST)
        .url("https://example.test/chat")
        .active(true)
        .createdBy(creator)
        .build();
  }

  @SuppressWarnings("unchecked")
  private <T> T ignoredRepository(Class<T> type) {
    return (T)
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (proxy, method, args) -> {
              throw new UnsupportedOperationException(method.getName());
            });
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }
}
