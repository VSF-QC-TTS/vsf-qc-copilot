package me.nghlong3004.vqc.api.mail.service;

import me.nghlong3004.vqc.api.mail.model.MailRequest;
import me.nghlong3004.vqc.api.mail.model.MailType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface MailService {

  void send(MailType type, MailRequest request);
}
