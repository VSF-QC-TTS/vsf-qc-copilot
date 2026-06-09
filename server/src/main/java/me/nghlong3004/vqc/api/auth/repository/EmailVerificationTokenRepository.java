package me.nghlong3004.vqc.api.auth.repository;

import java.util.Optional;
import me.nghlong3004.vqc.api.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, Long> {

  Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
