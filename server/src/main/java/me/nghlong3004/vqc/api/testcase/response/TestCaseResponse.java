package me.nghlong3004.vqc.api.testcase.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import me.nghlong3004.vqc.api.testcase.enums.TestCaseStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "TestCaseResponse", description = "Test case detail payload")
public record TestCaseResponse(
    @Schema(description = "Public test case identifier.") UUID publicId,
    @Schema(description = "Owning dataset public identifier.") UUID datasetPublicId,
    @Schema(description = "External/source row identifier.", nullable = true) String externalId,
    @Schema(description = "User question/input.", nullable = true) String question,
    @Schema(description = "Multi-turn conversation history.", nullable = true)
        java.util.List<me.nghlong3004.vqc.api.testcase.entity.TestCaseTurn> turns,
    @Schema(description = "Optional context/precondition.", nullable = true)
        Map<String, Object> precondition,
    @Schema(description = "Expected answer.", nullable = true) String groundTruth,
    @Schema(description = "Optional test metadata.", nullable = true) Map<String, Object> metadata,
    @Schema(description = "Test case lifecycle status.") TestCaseStatus status,
    @Schema(description = "Stable display order.", nullable = true) Integer sortOrder,
    @Schema(description = "Test case creation time.") OffsetDateTime createdAt,
    @Schema(description = "Last test case update time.") OffsetDateTime updatedAt) {}
