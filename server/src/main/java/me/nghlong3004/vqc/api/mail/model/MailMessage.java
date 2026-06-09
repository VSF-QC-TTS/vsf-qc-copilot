package me.nghlong3004.vqc.api.mail.model;

import java.util.Map;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public record MailMessage(String to, String subject, String templatePath, Map<String, ?> model) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String to;
    private String subject;
    private String templatePath;
    private Map<String, ?> model = Map.of();

    public Builder to(String to) {
      this.to = to;
      return this;
    }

    public Builder subject(String subject) {
      this.subject = subject;
      return this;
    }

    public Builder templatePath(String templatePath) {
      this.templatePath = templatePath;
      return this;
    }

    public Builder model(Map<String, ?> model) {
      this.model = model;
      return this;
    }

    public MailMessage build() {
      return new MailMessage(to, subject, templatePath, model);
    }
  }
}
