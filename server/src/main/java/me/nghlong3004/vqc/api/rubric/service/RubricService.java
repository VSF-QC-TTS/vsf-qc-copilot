package me.nghlong3004.vqc.api.rubric.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricRequest;
import me.nghlong3004.vqc.api.rubric.request.UpdateRubricRequest;
import me.nghlong3004.vqc.api.rubric.response.RubricPageResponse;
import me.nghlong3004.vqc.api.rubric.response.RubricResponse;
import org.springframework.data.domain.Pageable;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public interface RubricService {

  RubricPageResponse listMyRubrics(RubricStatus status, Pageable pageable, String username);

  RubricPageResponse listTemplates(Pageable pageable, String username);

  RubricResponse getRubric(UUID rubricPublicId, String username);

  RubricResponse updateRubric(UUID rubricPublicId, UpdateRubricRequest request, String username);

  void archiveRubric(UUID rubricPublicId, String username);

  RubricResponse cloneRubric(UUID rubricPublicId, String username);
}
