package me.nghlong3004.vqc.api.config;

import me.nghlong3004.vqc.api.mail.config.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {}
