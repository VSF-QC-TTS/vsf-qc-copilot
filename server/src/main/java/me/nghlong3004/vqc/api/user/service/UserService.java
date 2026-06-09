package me.nghlong3004.vqc.api.user.service;

import me.nghlong3004.vqc.api.auth.request.RegisterRequest;
import me.nghlong3004.vqc.api.user.response.UserResponse;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface UserService {
  UserResponse register(RegisterRequest request);
}
