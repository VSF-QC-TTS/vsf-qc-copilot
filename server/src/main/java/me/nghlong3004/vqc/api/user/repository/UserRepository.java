package me.nghlong3004.vqc.api.user.repository;

import java.util.Optional;
import me.nghlong3004.vqc.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByUsername(String username);

  Optional<User> findByUsername(String username);
}
