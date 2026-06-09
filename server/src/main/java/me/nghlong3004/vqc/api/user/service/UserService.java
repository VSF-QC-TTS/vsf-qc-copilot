package me.nghlong3004.vqc.api.user.service;

import jakarta.validation.Valid;
import me.nghlong3004.vqc.api.auth.request.RegisterRequest;
import me.nghlong3004.vqc.api.user.response.UserResponse;
import org.springframework.validation.annotation.Validated;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Validated
public interface UserService {
  UserResponse register(@Valid RegisterRequest request);
}
