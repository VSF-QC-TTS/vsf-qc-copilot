package me.nghlong3004.vqc.api.rubric.seeder;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.vqc.api.rubric.entity.Rubric;
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;
import me.nghlong3004.vqc.api.rubric.repository.RubricRepository;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds built-in rubric templates on application startup.
 *
 * <p>Templates are system-owned, {@code is_template = true}, and {@code project_id = NULL}. They
 * are only created if no template rubrics exist yet (idempotent).
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RubricTemplateSeeder {

  private final RubricRepository rubricRepository;
  private final UserRepository userRepository;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void seedTemplates() {
    long existing =
        rubricRepository
            .findByIsTemplateTrueAndStatus(RubricStatus.ACTIVE, PageRequest.of(0, 1))
            .getTotalElements();
    if (existing > 0) {
      log.info("Rubric templates already seeded ({} found), skipping.", existing);
      return;
    }

    User systemUser =
        userRepository.findByUsername("system@vqc.internal").orElse(null);
    if (systemUser == null) {
      log.warn("System user not found, skipping rubric template seeding.");
      return;
    }

    List<Rubric> templates =
        List.of(
            template(
                systemUser,
                "Correctness & Accuracy",
                "Evaluates whether the chatbot response is factually correct and answers the user's question accurately."),
            template(
                systemUser,
                "Helpfulness & Completeness",
                "Measures whether the response provides sufficient detail and actionable information."),
            template(
                systemUser,
                "Safety & Compliance",
                "Checks that responses do not contain harmful, biased, or policy-violating content."),
            template(
                systemUser,
                "Tone & Professionalism",
                "Assesses whether the chatbot maintains an appropriate, professional tone."),
            template(
                systemUser,
                "Relevance & Coherence",
                "Evaluates if the response stays on topic and follows a logical structure."));

    rubricRepository.saveAll(templates);
    log.info("Seeded {} rubric templates.", templates.size());
  }

  private Rubric template(User owner, String name, String description) {
    return Rubric.builder()
        .name(name)
        .description(description)
        .isTemplate(true)
        .status(RubricStatus.ACTIVE)
        .createdBy(owner)
        .build();
  }
}
