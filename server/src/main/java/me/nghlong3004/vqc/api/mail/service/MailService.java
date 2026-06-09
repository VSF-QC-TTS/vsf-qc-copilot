package me.nghlong3004.vqc.api.mail.service;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface MailService {

  void sendRegistrationWelcome(String to, String displayName);
}
