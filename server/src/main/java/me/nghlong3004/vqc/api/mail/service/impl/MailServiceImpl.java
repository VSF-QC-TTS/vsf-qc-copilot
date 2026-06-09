package me.nghlong3004.vqc.api.mail.service.impl;

import jakarta.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.mail.config.MailProperties;
import me.nghlong3004.vqc.api.mail.factory.MailStrategyFactory;
import me.nghlong3004.vqc.api.mail.model.MailMessage;
import me.nghlong3004.vqc.api.mail.model.MailRequest;
import me.nghlong3004.vqc.api.mail.model.MailType;
import me.nghlong3004.vqc.api.mail.service.MailService;
import me.nghlong3004.vqc.api.mail.template.HtmlMailTemplateRenderer;
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

  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;
  private final HtmlMailTemplateRenderer templateRenderer;
  private final MailStrategyFactory mailStrategyFactory;

  @Async
  @Override
  public void send(MailType type, MailRequest request) {
    try {
      MailMessage mailMessage = mailStrategyFactory.get(type).buildMessage(request);
      var message = mailSender.createMimeMessage();
      var helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setFrom(mailProperties.from());
      helper.setTo(mailMessage.to());
      helper.setSubject(mailMessage.subject());
      helper.setText(render(mailMessage), true);

      mailSender.send(message);
      log.info("Sent {} mail message", type);
    } catch (IllegalStateException | MailException | MessagingException ex) {
      log.warn("Failed to send {} mail message: {}", type, ex.getMessage());
    }
  }

  private String render(MailMessage mailMessage) {
    return templateRenderer.render(mailMessage.templatePath(), mailMessage.model());
  }
}
