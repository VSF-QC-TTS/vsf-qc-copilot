package me.nghlong3004.vqc.api.testcase.entity;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single turn in a multi-turn conversation test case.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/17/2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseTurn {
  private String role; // e.g., "user", "assistant"
  private String content; // The message content
  private Map<String, Object> metadata; // Optional turn-specific metadata
}
