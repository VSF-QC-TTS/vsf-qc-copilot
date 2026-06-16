package me.nghlong3004.vqc.api.mail.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import me.nghlong3004.vqc.api.mail.model.MailRequest;
import me.nghlong3004.vqc.api.mail.model.MailType;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
class EmailVerificationMailStrategyTest {

  private final EmailVerificationMailStrategy strategy = new EmailVerificationMailStrategy();

  @Test
  void buildMessageUsesEmailVerificationTemplateAndModel() {
    var request =
        MailRequest.builder()
            .to("qc.demo@example.com")
            .displayName("QC Demo")
            .actionUrl("https://app.example.test/verify-email?token=abc")
            .build();

    var message = strategy.buildMessage(request);

    assertThat(strategy.type()).isEqualTo(MailType.EMAIL_VERIFICATION);
    assertThat(message.to()).isEqualTo("qc.demo@example.com");
    assertThat(message.subject()).isEqualTo("Verify your VF QC Copilot account");
    assertThat(message.templatePath()).isEqualTo("templates/mail/email-verification.html");
    assertThat(message.model().get("displayName")).isEqualTo("QC Demo");
    assertThat(message.model().get("verificationUrl"))
        .isEqualTo("https://app.example.test/verify-email?token=abc");
  }
}
