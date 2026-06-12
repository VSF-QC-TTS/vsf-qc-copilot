package me.nghlong3004.vqc.api.evaluation.promptfoo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Map;
import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
class PromptfooConfigGeneratorTest {

  private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
  private final PromptfooConfigGenerator generator = new PromptfooConfigGenerator(objectMapper);

  @Test
  void configIncludesTimeoutAsEvaluateOptions() {
    var run = run(60, 0);
    Map<String, Object> config = generator.config(run);
    assertThat(config).containsKey("evaluateOptions");
    @SuppressWarnings("unchecked")
    Map<String, Object> evaluateOptions = (Map<String, Object>) config.get("evaluateOptions");
    assertThat(evaluateOptions).containsEntry("timeoutMs", 60_000L);
  }

  @Test
  void configOmitsEvaluateOptionsWhenTimeoutIsZero() {
    var connector =
        TargetApiConnector.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .project(Project.builder().id(1L).build())
            .name("no-timeout")
            .method(HttpMethodType.POST)
            .url("https://example.test/chat")
            .bodyTemplate(Map.of("q", "{{question}}"))
            .responseSelector("$.answer")
            .timeoutSeconds(0)
            .retryCount(0)
            .build();
    var run =
        EvaluationRun.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .dataset(Dataset.builder().id(1L).build())
            .rubricVersion(RubricVersion.builder().id(1L).build())
            .targetApiConnector(connector)
            .build();
    Map<String, Object> config = generator.config(run);
    assertThat(config).doesNotContainKey("evaluateOptions");
  }

  @Test
  @SuppressWarnings("unchecked")
  void providerConfigIncludesMaxRetries() {
    var run = run(30, 3);
    Map<String, Object> config = generator.config(run);
    var providers = (java.util.List<Map<String, Object>>) config.get("providers");
    var providerConfig = (Map<String, Object>) providers.get(0).get("config");
    assertThat(providerConfig).containsEntry("maxRetries", 3);
  }

  @Test
  @SuppressWarnings("unchecked")
  void providerConfigOmitsMaxRetriesWhenZero() {
    var run = run(30, 0);
    Map<String, Object> config = generator.config(run);
    var providers = (java.util.List<Map<String, Object>>) config.get("providers");
    var providerConfig = (Map<String, Object>) providers.get(0).get("config");
    assertThat(providerConfig).doesNotContainKey("maxRetries");
  }

  private EvaluationRun run(int timeoutSeconds, int retryCount) {
    return EvaluationRun.builder()
        .id(1L)
        .publicId(UUID.randomUUID())
        .dataset(Dataset.builder().id(1L).build())
        .rubricVersion(RubricVersion.builder().id(1L).build())
        .targetApiConnector(
            TargetApiConnector.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .project(Project.builder().id(1L).build())
                .name("Test connector")
                .method(HttpMethodType.POST)
                .url("https://example.test/chat")
                .bodyTemplate(Map.of("question", "{{question}}"))
                .responseSelector("$.answer")
                .timeoutSeconds(timeoutSeconds)
                .retryCount(retryCount)
                .build())
        .build();
  }
}
