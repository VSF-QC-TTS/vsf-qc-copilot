package me.nghlong3004.vqc.api.rubric.service;

import me.nghlong3004.vqc.api.rubric.request.CreateRubricWithVersionRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionResponse;

/**
 * Workflow-level rubric operations that coordinate rubric, version, and criteria writes.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
public interface RubricWorkflowService {

  RubricVersionResponse createRubricWithVersion(CreateRubricWithVersionRequest request, String username);
}
