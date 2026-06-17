# spring-boot Skills Index

## File Match (auto-check against the file you are editing)

| Skill | File pattern | Keywords |
| ----- | ------------ | -------- |
| **spring-boot-api-design** | `**/*Controller.java` | openapi, swagger, versioning, problemdetails |
| **spring-boot-architecture** | `pom.xml`, `build.gradle` | structure, layering, dto, controller, @RestController, @Service, @Repository, @Entity, @Bean, @Configuration |
| **spring-boot-best-practices** | `application.properties`, `**/*Service.java` | autowired, requiredargsconstructor, configuration-properties, slf4j |
| **spring-boot-data-access** | `**/*Repository.java`, `**/*Entity.java` | jpa-repository, entity-graph, transactional, n-plus-1 |
| **spring-boot-deployment** | `compose.yml` | Dockerfile, docker-layer, native-image, graceful-shutdown |
| **spring-boot-observability** | `logback-spring.xml`, `application.properties` | micrometer, tracing, correlation-id, mdc |
| **spring-boot-scheduling** | `**/*Scheduler.java`, `**/*Job.java` | scheduled, shedlock, cron |
| **spring-boot-security** | `**/*SecurityConfig.java`, `**/*Filter.java` | security-filter-chain, lambda-dsl, csrf, cors |
| **spring-boot-testing** | `**/*Test.java` | webmvctest, datajpatest, testcontainers, assertj |

## close to matching keywords

| Skill | Match when user mentions |
| ----- | ----------------------- |
| java-best-practices | refactor, SOLID, builder, factory, composition, immutable, Optional, checked exception, clean code |
| java-concurrency | Thread, Executor, synchronized, lock, CompletableFuture, StructuredTaskScope, VirtualThread, AtomicInteger, async, race condition |
| **common-owasp** | security review, OWASP, broken access control, IDOR, BOLA, injection, broken auth, API review, authorization, access control, mobile security |

> Load matched skills: `.agents/skills/backend/spring-boot/<skill>/SKILL.md`. Load ALL that match — the tier model already filters irrelevant ones.
