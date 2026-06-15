package me.nghlong3004.vqc.api.rubric.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricCriterionRequest;
import org.junit.jupiter.api.Test;

class RubricGenerationServiceImplTest {

  @Test
  void toCriterionRequestsSanitizesMetricKeysAndKeepsThemUnique() {
    RubricGenerationServiceImpl.GeneratedRubric generated =
        new RubricGenerationServiceImpl.GeneratedRubric(
            "Rubric",
            "Description",
            "Content",
            null,
            List.of(
                generatedCriterion("Độ chính xác", 1),
                generatedCriterion("Độ chính xác", 2),
                generatedCriterion("123 Tone!", 3),
                generatedCriterion(null, 4)));

    List<CreateRubricCriterionRequest> criteria =
        RubricGenerationServiceImpl.toCriterionRequests(generated);

    assertThat(criteria)
        .extracting(CreateRubricCriterionRequest::metricKey)
        .containsExactly("do_chinh_xac", "do_chinh_xac_2", "metric_123_tone", "criterion_4");
  }

  private RubricGenerationServiceImpl.GeneratedCriterion generatedCriterion(
      String metricKey, int sortOrder) {
    return new RubricGenerationServiceImpl.GeneratedCriterion(
        "Criterion " + sortOrder,
        "Description",
        200,
        "Pass",
        "Fail",
        "Judge semantically against the expected answer.",
        metricKey,
        false,
        sortOrder);
  }
}
