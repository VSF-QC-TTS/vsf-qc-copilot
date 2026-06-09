package me.nghlong3004.vqc.api.project.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.project.enums.ProjectStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "ProjectResponse", description = "Project detail payload")
public record ProjectResponse(
    @Schema(
            description = "Public project identifier. Internal numeric ids are never exposed.",
            example = "5a4edcc1-cd1e-44ef-a144-31f5f3d2f653")
        UUID publicId,
    @Schema(description = "Project name.", example = "AI Health Chatbot Demo") String name,
    @Schema(
            description = "Project description.",
            example = "Evaluate health chatbot answers for Apple Health-like scenarios.",
            nullable = true)
        String description,
    @Schema(
            description = "What this project evaluates.",
            example = "Health assistant QA evaluation",
            nullable = true)
        String evaluationScope,
    @Schema(description = "Retention period for evaluation artifacts.", example = "30")
        Integer retentionDays,
    @Schema(description = "Project lifecycle status.", example = "ACTIVE") ProjectStatus status,
    @Schema(description = "Project creator.") ProjectCreatorResponse createdBy,
    @Schema(description = "Project creation time.", example = "2026-06-08T10:30:00+07:00")
        OffsetDateTime createdAt,
    @Schema(description = "Last project update time.", example = "2026-06-08T10:30:00+07:00")
        OffsetDateTime updatedAt) {}
