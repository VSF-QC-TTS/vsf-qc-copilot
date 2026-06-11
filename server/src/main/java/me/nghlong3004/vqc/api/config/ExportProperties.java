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
@ConfigurationProperties(prefix = "vqc.export")
public class ExportProperties {

  /** Directory where generated export files are stored. */
  private String dir = "./exports";
}
