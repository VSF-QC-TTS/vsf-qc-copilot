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
@ConfigurationProperties(prefix = "vqc.storage")
public class StorageProperties {

  /** Storage implementation type. */
  private String type = "local";

  /** Local filesystem storage settings. */
  private Local local = new Local();

  @Getter
  @Setter
  public static class Local {

    /** Directory where objects are stored for local storage. */
    private String baseDir = "./exports";
  }
}
