package me.nghlong3004.vqc.api.mail.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

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
class PasswordResetMailStrategyTest {

  @Mock private MessageSource messageSource;
  private PasswordResetMailStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new PasswordResetMailStrategy(messageSource);
  }

  @Test
  void buildMessageUsesPasswordResetTemplateAndModel() {
    lenient().when(messageSource.getMessage(any(String.class), any(), any(Locale.class)))
        .thenAnswer(inv -> String.valueOf(inv.getArgument(0)));
    lenient().when(messageSource.getMessage(eq("mail.reset.subject"), any(), any()))
        .thenReturn("Reset your VF QC Copilot password");
    lenient().when(messageSource.getMessage(eq("mail.reset.greeting"), any(), any()))
        .thenReturn("Hi QC Demo,");

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
    assertThat(message.model().get("appName")).isEqualTo("VF QC Copilot");
    assertThat(message.model().get("greeting")).isEqualTo("Hi QC Demo,");
    assertThat(message.model().get("actionUrl"))
        .isEqualTo("https://app.example.test/reset-password?token=abc");
  }
}
