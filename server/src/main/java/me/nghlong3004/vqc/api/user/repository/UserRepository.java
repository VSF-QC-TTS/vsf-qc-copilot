package me.nghlong3004.vqc.api.user.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Checks whether a normalized email is already registered as a {@link User}.
   *
   * @param username normalized email stored in the username column
   * @return true when the user exists
   */
  boolean existsByUsername(String username);

  /**
   * Finds a {@link User} by normalized email.
   *
   * @param username normalized email stored in the username column
   * @return {@link Optional} containing the matching {@link User} when present
   */
  Optional<User> findByUsername(String username);

  /**
   * Finds a {@link User} by public identifier.
   *
   * @param publicId public user identifier
   * @return {@link Optional} containing the matching {@link User} when present
   */
  Optional<User> findByPublicId(UUID publicId);
}
