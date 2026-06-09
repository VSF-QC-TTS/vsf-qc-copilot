package me.nghlong3004.vqc.api.auth.service;

import me.nghlong3004.vqc.api.user.entity.User;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface PasswordResetService {

  /**
   * Creates a one-time password reset token for a {@link User}.
   *
   * @param user {@link User} requesting password reset
   * @return raw token sent to the {@link User} by email
   */
  String createResetToken(User user);

  /**
   * Replaces the related {@link User}'s password when the reset token is valid.
   *
   * @param rawToken raw reset token from the email link
   * @param newPassword new plain-text password to hash and persist
   */
  void resetPassword(String rawToken, String newPassword);
}
