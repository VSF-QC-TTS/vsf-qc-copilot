package me.nghlong3004.vqc.api.mail.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import me.nghlong3004.vqc.api.mail.model.MailRequest;
import me.nghlong3004.vqc.api.mail.model.MailType;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
class PasswordResetMailStrategyTest {

  private final PasswordResetMailStrategy strategy = new PasswordResetMailStrategy();

  @Test
  void buildMessageUsesPasswordResetTemplateAndModel() {
    var request =
        MailRequest.builder()
            .to("qc.demo@example.com")
            .displayName("QC Demo")
            .actionUrl("https://app.example.test/reset-password?token=abc")
            .build();

    var message = strategy.buildMessage(request);

    assertThat(strategy.type()).isEqualTo(MailType.PASSWORD_RESET);
    assertThat(message.to()).isEqualTo("qc.demo@example.com");
    assertThat(message.subject()).isEqualTo("Reset your VF QC Copilot password");
    assertThat(message.templatePath()).isEqualTo("templates/mail/password-reset.html");
    assertThat(message.model().get("displayName")).isEqualTo("QC Demo");
    assertThat(message.model().get("resetUrl"))
        .isEqualTo("https://app.example.test/reset-password?token=abc");
  }
}
