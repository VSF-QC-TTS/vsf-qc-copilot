package me.nghlong3004.vqc.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vqc.promptfoo")
public class PromptfooProperties {

  /** Execution mode: "mock" or "cli". */
  private String mode = "mock";

  /** Working directory for promptfoo run files. */
  private String workDir = "./runs";

  /** Command to execute promptfoo CLI. */
  private String command = "./infra/scripts/run-promptfoo.sh";

  /** Maximum concurrent evaluations. */
  private int maxConcurrency = 1;
}
