package me.nghlong3004.vqc.api.rubric.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricVersionRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricVersionResponse;
import org.springframework.data.domain.Pageable;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface RubricVersionService {

  RubricVersionResponse createVersion(UUID rubricPublicId, String username);

  RubricVersionPageResponse listVersions(
      UUID rubricPublicId, RubricVersionStatus status, Pageable pageable, String username);

  RubricVersionResponse getVersion(UUID rubricVersionPublicId, String username);

  RubricVersionResponse updateVersion(
      UUID rubricVersionPublicId, UpdateRubricVersionRequest request, String username);
}
