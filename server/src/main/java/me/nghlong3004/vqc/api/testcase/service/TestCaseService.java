package me.nghlong3004.vqc.api.testcase.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.testcase.request.CreateTestCaseRequest;
import me.nghlong3004.vqc.api.testcase.response.TestCaseResponse;

/**
 * Coordinates test case use cases.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface TestCaseService {

  /**
   * Creates a test case under a dataset owned by the authenticated user.
   *
   * @param datasetPublicId public dataset identifier
   * @param request create test case payload
   * @param username authenticated principal username/email
   * @return created test case response
   */
  TestCaseResponse createTestCase(
      UUID datasetPublicId, CreateTestCaseRequest request, String username);
}
