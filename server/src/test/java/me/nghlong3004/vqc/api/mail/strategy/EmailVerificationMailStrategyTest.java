package me.nghlong3004.vqc.api.mail.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import me.nghlong3004.vqc.api.mail.model.MailRequest;
import me.nghlong3004.vqc.api.mail.model.MailType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import java.util.Locale;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationMailStrategyTest {

  @Mock private MessageSource messageSource;
  private EmailVerificationMailStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new EmailVerificationMailStrategy(messageSource);
  }

  @Test
  void buildMessageUsesEmailVerificationTemplateAndModel() {
    when(messageSource.getMessage(eq("mail.verify.subject"), any(), any()))
        .thenReturn("Verify your VF QC Copilot account");
    when(messageSource.getMessage(eq("mail.verify.greeting"), any(), any()))
        .thenReturn("Hi QC Demo,");

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
    assertThat(message.model().get("appName")).isEqualTo("VF QC Copilot");
    assertThat(message.model().get("greeting")).isEqualTo("Hi QC Demo,");
    assertThat(message.model().get("actionUrl"))
        .isEqualTo("https://app.example.test/verify-email?token=abc");
  }
}
