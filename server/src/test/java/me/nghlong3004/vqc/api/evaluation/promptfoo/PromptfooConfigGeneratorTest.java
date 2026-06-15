package me.nghlong3004.vqc.api.evaluation.promptfoo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.nghlong3004.vqc.api.dataset.entity.Dataset;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.rubric.entity.RubricCriterion;
import me.nghlong3004.vqc.api.rubric.entity.RubricVersion;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import me.nghlong3004.vqc.api.testcase.entity.TestCase;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
class PromptfooConfigGeneratorTest {

  private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
  private final PromptfooConfigGenerator generator =
      new PromptfooConfigGenerator(objectMapper, new RubricAssertionMapper());

  @Test
  void configIncludesTimeoutAsEvaluateOptions() {
    var run = run(60, 0);
    Map<String, Object> config = generator.config(run, List.of(), null);
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
    Map<String, Object> config = generator.config(run, List.of(), null);
    assertThat(config).doesNotContainKey("evaluateOptions");
  }

  @Test
  @SuppressWarnings("unchecked")
  void providerConfigIncludesMaxRetries() {
    var run = run(30, 3);
    Map<String, Object> config = generator.config(run, List.of(), null);
    var providers = (java.util.List<Map<String, Object>>) config.get("providers");
    var providerConfig = (Map<String, Object>) providers.get(0).get("config");
    assertThat(providerConfig).containsEntry("maxRetries", 3);
  }

  @Test
  @SuppressWarnings("unchecked")
  void providerConfigOmitsMaxRetriesWhenZero() {
    var run = run(30, 0);
    Map<String, Object> config = generator.config(run, List.of(), null);
    var providers = (java.util.List<Map<String, Object>>) config.get("providers");
    var providerConfig = (Map<String, Object>) providers.get(0).get("config");
    assertThat(providerConfig).doesNotContainKey("maxRetries");
  }

  @Test
  void transformResponseSupportsNestedArrayJsonPath() {
    assertThat(generator.transformResponse("$.candidates[0].content.parts[0].text"))
        .isEqualTo("json?.candidates?.[0]?.content?.parts?.[0]?.text");
  }

  @Test
  @SuppressWarnings("unchecked")
  void testsUseGroundTruthAsRubricContextWhenRubricAssertionsExist() {
    TestCase testCase =
        TestCase.builder()
            .id(11L)
            .publicId(UUID.randomUUID())
            .question("How many steps did I walk?")
            .groundTruth("The user walked 8,200 steps.")
            .build();
    List<Map<String, Object>> rubricAssertions =
        List.of(Map.of("type", "llm-rubric", "value", "Judge semantically", "metric", "accuracy"));

    List<Map<String, Object>> tests = generator.tests(List.of(testCase), rubricAssertions);

    Map<String, Object> test = tests.getFirst();
    Map<String, Object> vars = (Map<String, Object>) test.get("vars");
    assertThat(vars).containsEntry("groundTruth", "The user walked 8,200 steps.");
    List<Map<String, Object>> assertions = (List<Map<String, Object>>) test.get("assert");
    assertThat(assertions).hasSize(1);
    assertThat(assertions.getFirst()).containsEntry("type", "llm-rubric");
  }

  @Test
  @SuppressWarnings("unchecked")
  void testsUseContainsOnlyWhenNoRubricAssertionsExist() {
    TestCase testCase =
        TestCase.builder()
            .id(11L)
            .publicId(UUID.randomUUID())
            .question("How many steps did I walk?")
            .groundTruth("The user walked 8,200 steps.")
            .build();

    List<Map<String, Object>> tests = generator.tests(List.of(testCase), List.of());

    Map<String, Object> test = tests.getFirst();
    Map<String, Object> vars = (Map<String, Object>) test.get("vars");
    assertThat(vars).containsEntry("groundTruth", "The user walked 8,200 steps.");
    List<Map<String, Object>> assertions = (List<Map<String, Object>>) test.get("assert");
    assertThat(assertions).containsExactly(Map.of("type", "contains", "value", "The user walked 8,200 steps."));
  }

  @Test
  void generatedRubricAssertionsIncludeVersionContent() throws Exception {
    var run = run(30, 0);
    run.setRubricVersion(
        RubricVersion.builder()
            .id(1L)
            .content("Overall context: judge by semantic equivalence.")
            .build());
    RubricCriterion criterion =
        RubricCriterion.builder()
            .metricKey("accuracy")
            .name("Accuracy")
            .judgeInstruction("Check accuracy")
            .weight(1)
            .build();

    java.nio.file.Path runDir = java.nio.file.Files.createTempDirectory("promptfoo-config-test");
    generator.generate(run, List.of(testCase()), List.of(criterion), "google:gemini-2.5-flash", runDir);

    com.fasterxml.jackson.databind.JsonNode tests =
        objectMapper.readTree(runDir.resolve("tests.json").toFile());
    String value = tests.get(0).path("assert").get(0).path("value").asText();
    assertThat(value).contains("Overall context: judge by semantic equivalence.");
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

  private TestCase testCase() {
    return TestCase.builder()
        .id(11L)
        .publicId(UUID.randomUUID())
        .question("Question")
        .groundTruth("Expected")
        .build();
  }
}
