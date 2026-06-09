package me.nghlong3004.vqc.api.auth.service;

import me.nghlong3004.vqc.api.user.entity.User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface EmailVerificationService {

  /**
   * Creates a one-time email verification token for a pending {@link User}.
   *
   * @param user {@link User} that must verify email ownership
   * @return raw token sent to the {@link User} by email
   */
  String createVerificationToken(User user);

  /**
   * Marks the related {@link User} active when the token is valid.
   *
   * @param rawToken raw verification token from the email link
   * @return activated {@link User}
   */
  User verifyEmail(String rawToken);
}
