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
public class PasswordResetMailStrategy implements MailStrategy {

  private static final String TEMPLATE_PATH = "templates/mail/password-reset.html";
  private static final String SUBJECT = "Reset your VF QC Copilot password";

  @Override
  public MailType type() {
    return MailType.PASSWORD_RESET;
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
                "resetUrl", request.actionUrl(),
                "preheader", "Use this secure link to reset your VF QC Copilot password."))
        .build();
  }
}
