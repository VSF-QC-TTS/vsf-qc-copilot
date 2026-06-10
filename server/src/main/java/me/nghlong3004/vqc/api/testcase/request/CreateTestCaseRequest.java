package me.nghlong3004.vqc.api.testcase.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "CreateTestCaseRequest", description = "Create test case payload")
public record CreateTestCaseRequest(
    @Schema(description = "External/source row identifier.", example = "HEALTH_001", nullable = true)
        @Size(max = 255, message = "External ID must be at most 255 characters.")
        String externalId,
    @Schema(description = "User question/input.", example = "How many steps did I walk today?")
        @NotBlank(message = "Test case question is required.")
        String question,
    @Schema(description = "Optional context/precondition.", nullable = true)
        Map<String, Object> precondition,
    @Schema(description = "Expected answer.", example = "The user walked 8,200 steps today.", nullable = true)
        String groundTruth,
    @Schema(description = "Optional test metadata.", nullable = true)
        Map<String, Object> metadata,
    @Schema(description = "Test case lifecycle status.", example = "ACTIVE", nullable = true)
        TestCaseStatus status,
    @Schema(description = "Stable display order.", example = "1", nullable = true)
        Integer sortOrder) {}
