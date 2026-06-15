package me.nghlong3004.vqc.api.judge.mapper;

import me.nghlong3004.vqc.api.judge.entity.JudgeModel;
import me.nghlong3004.vqc.api.judge.response.JudgeModelResponse;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Component
public class JudgeModelMapper {

  public JudgeModelResponse toResponse(JudgeModel judgeModel) {
    return new JudgeModelResponse(
        judgeModel.getPublicId(),
        judgeModel.getProject().getPublicId(),
        judgeModel.getName(),
        judgeModel.getProvider(),
        judgeModel.getModelName(),
        judgeModel.getBaseUrl(),
        judgeModel.getApiKeyMasked(),
        judgeModel.getConfigJson(),
        Boolean.TRUE.equals(judgeModel.getActive()),
        judgeModel.getCreatedAt(),
        judgeModel.getUpdatedAt());
  }
}
