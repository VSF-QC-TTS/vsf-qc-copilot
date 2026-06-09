package me.nghlong3004.vqc.api.auth.service;

import jakarta.validation.Valid;
import me.nghlong3004.vqc.api.auth.request.ForgotPasswordRequest;
import me.nghlong3004.vqc.api.auth.request.LoginRequest;
import me.nghlong3004.vqc.api.auth.request.ResetPasswordRequest;
import me.nghlong3004.vqc.api.auth.request.VerifyEmailRequest;
import me.nghlong3004.vqc.api.auth.response.LoginResult;
import me.nghlong3004.vqc.api.user.response.UserResponse;
import org.springframework.validation.annotation.Validated;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Validated
public interface AuthService {

  /**
   * Authenticates a local account from a {@link LoginRequest} and creates access/refresh tokens.
   *
   * @param request validated {@link LoginRequest}
   * @return {@link LoginResult} containing response data and refresh token cookie metadata
   */
  LoginResult login(@Valid LoginRequest request);

  /**
   * Verifies a pending account email address using a token from {@link VerifyEmailRequest}.
   *
   * @param request validated {@link VerifyEmailRequest}
   * @return activated {@link UserResponse}
   */
  UserResponse verifyEmail(@Valid VerifyEmailRequest request);

  /**
   * Starts the password reset flow from a {@link ForgotPasswordRequest}.
   *
   * @param request validated {@link ForgotPasswordRequest}
   */
  void forgotPassword(@Valid ForgotPasswordRequest request);

  /**
   * Sets a new password using a valid one-time token from {@link ResetPasswordRequest}.
   *
   * @param request validated {@link ResetPasswordRequest}
   */
  void resetPassword(@Valid ResetPasswordRequest request);
}
