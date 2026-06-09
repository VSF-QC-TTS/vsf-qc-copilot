package me.nghlong3004.vqc.api.mail.model;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public record MailRequest(String to, String displayName, String verificationUrl) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String to;
    private String displayName;
    private String verificationUrl;

    public Builder to(String to) {
      this.to = to;
      return this;
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder verificationUrl(String verificationUrl) {
      this.verificationUrl = verificationUrl;
      return this;
    }

    public MailRequest build() {
      return new MailRequest(to, displayName, verificationUrl);
    }
  }
}
