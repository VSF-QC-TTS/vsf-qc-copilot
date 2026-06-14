package me.nghlong3004.vqc.api.targetconnector.curl;

import java.util.Map;

/**
 * Result of detecting secrets within connector headers.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/14/2026
 */
public record SecretDetectionResult(
    Map<String, String> sanitizedHeaders,
    Map<String, String> secretValues) {}
