package me.nghlong3004.vqc.api.rubric.service;

import me.nghlong3004.vqc.api.rubric.request.GenerateRubricPreviewRequest;
import me.nghlong3004.vqc.api.rubric.response.GenerateRubricPreviewResponse;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
public interface RubricGenerationService {

  GenerateRubricPreviewResponse generatePreview(
      GenerateRubricPreviewRequest request, String username);
}
