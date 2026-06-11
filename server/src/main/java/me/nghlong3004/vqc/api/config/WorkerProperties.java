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
@ConfigurationProperties(prefix = "vqc.worker")
public class WorkerProperties {

  /** Whether the job worker consumer is enabled. */
  private boolean enabled = true;

  /** Redis queue key for job dispatching. */
  private String queueKey = "vqc:jobs:queue";
}
