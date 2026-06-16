package me.nghlong3004.vqc.api.mail.strategy;

import java.util.Map;
import me.nghlong3004.vqc.api.mail.model.MailMessage;
import me.nghlong3004.vqc.api.mail.model.MailRequest;
import me.nghlong3004.vqc.api.mail.model.MailType;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Component
public class EmailVerificationMailStrategy implements MailStrategy {

  private static final String TEMPLATE_PATH = "templates/mail/email-verification.html";
  private static final String SUBJECT = "Verify your VF QC Copilot account";

  @Override
  public MailType type() {
    return MailType.EMAIL_VERIFICATION;
  }

  @Override
  public MailMessage buildMessage(MailRequest request) {
    return MailMessage.builder()
        .to(request.to())
        .subject(SUBJECT)
        .templatePath(TEMPLATE_PATH)
        .model(
            Map.of(
                "appName", "VF QC Copilot",
                "displayName", request.displayName(),
                "verificationUrl", request.actionUrl(),
                "preheader",
                    "Verify your email address to activate your VF QC Copilot account."))
        .build();
  }
}
