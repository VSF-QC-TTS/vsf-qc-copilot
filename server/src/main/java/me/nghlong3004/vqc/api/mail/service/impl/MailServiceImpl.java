package me.nghlong3004.vqc.api.mail.service.impl;

import jakarta.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.mail.config.MailProperties;
import me.nghlong3004.vqc.api.mail.service.MailService;
import me.nghlong3004.vqc.api.mail.template.HtmlMailTemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

  private static final String REGISTRATION_WELCOME_TEMPLATE =
      "templates/mail/registration-welcome.html";
  private static final String REGISTRATION_WELCOME_SUBJECT = "Welcome to VSF QC Copilot";

  @Value("${vqc.client.base-url}")
  private String clientBaseUrl;

  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;
  private final HtmlMailTemplateRenderer templateRenderer;

  @Async
  @Override
  public void sendRegistrationWelcome(String to, String displayName) {
    try {
      var message = mailSender.createMimeMessage();
      var helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setFrom(mailProperties.from());
      helper.setTo(to);
      helper.setSubject(REGISTRATION_WELCOME_SUBJECT);
      helper.setText(renderRegistrationWelcome(displayName), true);

      mailSender.send(message);
      log.info("Sent registration welcome email");
    } catch (IllegalStateException | MailException | MessagingException ex) {
      log.warn("Failed to send registration welcome email: {}", ex.getMessage());
    }
  }

  private String renderRegistrationWelcome(String displayName) {
    return templateRenderer.render(
        REGISTRATION_WELCOME_TEMPLATE,
        Map.of(
            "appName", "VSF QC Copilot",
            "displayName", displayName,
            "dashboardUrl", clientBaseUrl,
            "preheader", "Your VSF QC Copilot workspace is ready."));
  }
}
