package me.nghlong3004.vqc.api.judge.service;

import java.util.UUID;
import me.nghlong3004.vqc.api.judge.request.CreateJudgeModelRequest;
import me.nghlong3004.vqc.api.judge.request.UpdateJudgeModelRequest;
import me.nghlong3004.vqc.api.judge.response.JudgeModelPageResponse;
import me.nghlong3004.vqc.api.judge.response.JudgeModelResponse;
import org.springframework.data.domain.Pageable;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
public interface JudgeModelService {

  JudgeModelResponse createJudgeModel(
      UUID projectPublicId, CreateJudgeModelRequest request, String username);

  JudgeModelPageResponse listJudgeModels(
      UUID projectPublicId, Boolean active, Pageable pageable, String username);

  JudgeModelResponse updateJudgeModel(
      UUID judgeModelPublicId, UpdateJudgeModelRequest request, String username);

  JudgeModelResponse testConnection(UUID judgeModelPublicId, String username);

  void deleteJudgeModel(UUID judgeModelPublicId, String username);
}
