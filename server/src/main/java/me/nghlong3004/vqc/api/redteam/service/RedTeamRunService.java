package me.nghlong3004.vqc.api.redteam.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.redteam.request.CreateRedTeamRunRequest;
import me.nghlong3004.vqc.api.redteam.response.CreateRedTeamRunResponse;
import me.nghlong3004.vqc.api.redteam.response.RedTeamResultResponse;
import me.nghlong3004.vqc.api.redteam.response.RedTeamRunPageResponse;
import me.nghlong3004.vqc.api.redteam.response.RedTeamRunResponse;
import org.springframework.data.domain.Pageable;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
public interface RedTeamRunService {

  CreateRedTeamRunResponse createRedTeamRun(
      UUID projectPublicId, CreateRedTeamRunRequest request, String username);

  RedTeamRunPageResponse listRedTeamRuns(UUID projectPublicId, Pageable pageable, String username);

  RedTeamRunResponse getRedTeamRun(UUID runPublicId, String username);

  RedTeamResultResponse getRedTeamResults(UUID runPublicId, String username);
}
