package me.nghlong3004.vqc.api.auth.service;

import me.nghlong3004.vqc.api.user.entity.User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface EmailVerificationService {

  String createVerificationToken(User user);

  User verifyEmail(String rawToken);
}
