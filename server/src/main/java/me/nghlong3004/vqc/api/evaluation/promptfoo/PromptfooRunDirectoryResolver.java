package me.nghlong3004.vqc.api.evaluation.promptfoo;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.config.PromptfooProperties;
import me.nghlong3004.vqc.api.evaluation.entity.EvaluationRun;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
@Component
@RequiredArgsConstructor
public class PromptfooRunDirectoryResolver {

  private final PromptfooProperties promptfooProperties;

  public Path resolve(EvaluationRun run) {
    if (run.getPublicId() == null) {
      throw new PromptfooExecutionException("Evaluation run public ID is required.");
    }
    return Path.of(promptfooProperties.getWorkDir())
        .toAbsolutePath()
        .normalize()
        .resolve(run.getPublicId().toString())
        .normalize();
  }
}
