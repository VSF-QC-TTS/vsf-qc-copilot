package me.nghlong3004.vqc.api.auth.service;

import jakarta.validation.Valid;
import me.nghlong3004.vqc.api.auth.request.LoginRequest;
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

  LoginResult login(@Valid LoginRequest request);

  UserResponse verifyEmail(@Valid VerifyEmailRequest request);
}
