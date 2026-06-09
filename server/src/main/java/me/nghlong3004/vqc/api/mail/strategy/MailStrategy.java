package me.nghlong3004.vqc.api.mail.strategy;

import me.nghlong3004.vqc.api.mail.model.MailMessage;
import me.nghlong3004.vqc.api.mail.model.MailRequest;
import me.nghlong3004.vqc.api.mail.model.MailType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface MailStrategy {

  /**
   * Declares the {@link MailType} handled by this strategy.
   *
   * @return supported {@link MailType}
   */
  MailType type();

  /**
   * Builds the {@link MailMessage} metadata and template model.
   *
   * @param request {@link MailRequest} with recipient and scenario-specific data
   * @return renderable {@link MailMessage}
   */
  MailMessage buildMessage(MailRequest request);
}
