package me.nghlong3004.vqc.api.mail.strategy;

import me.nghlong3004.vqc.api.mail.model.MailMessage;
import me.nghlong3004.vqc.api.mail.model.MailRequest;
import me.nghlong3004.vqc.api.mail.model.MailType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface MailStrategy {

  MailType type();

  MailMessage buildMessage(MailRequest request);
}
