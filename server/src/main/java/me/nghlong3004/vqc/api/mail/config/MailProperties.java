package me.nghlong3004.vqc.api.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@ConfigurationProperties(prefix = "vqc.mail")
public record MailProperties(String from) {

  public MailProperties {
    if (from == null || from.isBlank()) {
      from = "no-reply@localhost";
    }
  }
}
