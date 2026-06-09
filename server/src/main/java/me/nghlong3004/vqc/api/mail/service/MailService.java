package me.nghlong3004.vqc.api.mail.service;

import me.nghlong3004.vqc.api.mail.model.MailRequest;
import me.nghlong3004.vqc.api.mail.model.MailType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface MailService {

  /**
   * Sends a typed HTML email using the strategy selected by {@link MailType}.
   *
   * @param type {@link MailType} scenario to send
   * @param request {@link MailRequest} containing recipient and scenario-specific model data
   */
  void send(MailType type, MailRequest request);
}
