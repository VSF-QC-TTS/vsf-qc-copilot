package me.nghlong3004.vqc.api.user.mapper;

import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

  /**
   * Maps an internal {@link User} entity to a public API response.
   *
   * @param user internal {@link User} entity
   * @return public {@link UserResponse}
   */
  @Mapping(source = "username", target = "email")
  UserResponse toResponse(User user);
}
