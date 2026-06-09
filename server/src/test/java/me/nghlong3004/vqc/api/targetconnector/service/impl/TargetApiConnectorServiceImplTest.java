package me.nghlong3004.vqc.api.targetconnector.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.project.entity.Project;
import me.nghlong3004.vqc.api.project.repository.ProjectRepository;
import me.nghlong3004.vqc.api.targetconnector.entity.TargetApiConnector;
import me.nghlong3004.vqc.api.targetconnector.enums.AuthType;
import me.nghlong3004.vqc.api.targetconnector.enums.BodyType;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import me.nghlong3004.vqc.api.targetconnector.enums.ResponseFormat;
import me.nghlong3004.vqc.api.targetconnector.mapper.TargetApiConnectorMapper;
import me.nghlong3004.vqc.api.targetconnector.repository.TargetApiConnectorRepository;
import me.nghlong3004.vqc.api.targetconnector.request.CreateTargetApiConnectorRequest;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorListItemResponse;
import me.nghlong3004.vqc.api.targetconnector.response.TargetApiConnectorPageResponse;
import me.nghlong3004.vqc.api.user.entity.User;
import me.nghlong3004.vqc.api.user.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
class TargetApiConnectorServiceImplTest {

  @Test
  void createConnectorPersistsSafeConfigAndMaskedSecretRefs() {
    User creator = new User();
    creator.setUsername("qc.demo@example.com");
    Project project = new Project();
    AtomicReference<String> lookedUpUsername = new AtomicReference<>();
    AtomicReference<ProjectLookup> projectLookup = new AtomicReference<>();
    AtomicReference<TargetApiConnector> savedConnector = new AtomicReference<>();
    TargetApiConnectorResponse mappedResponse =
        new TargetApiConnectorResponse(
            null, project.getPublicId(), "Mock Health Chatbot", null, HttpMethodType.POST, null,
            null, "http://localhost:8080/mock-chatbot/chat", Map.of(), Map.of(), Map.of(),
            BodyType.RAW_JSON, Map.of(), null, AuthType.BEARER, Map.of(), java.util.List.of(),
            ResponseFormat.JSON, "$.answer", false, null, null, 60, 1, true, null, null);
    TargetApiConnectorServiceImpl service =
        new TargetApiConnectorServiceImpl(
            connectorRepository(savedConnector),
            projectRepository(Optional.of(project), projectLookup),
            userRepository(Optional.of(creator), lookedUpUsername),
            mapper(mappedResponse));

    var response =
        service.createConnector(
            project.getPublicId(),
            request(),
            "  QC.Demo@Example.COM  ");

    assertThat(lookedUpUsername).hasValue("qc.demo@example.com");
    assertThat(projectLookup.get().publicId()).isEqualTo(project.getPublicId());
    assertThat(projectLookup.get().createdBy()).isSameAs(creator);
    assertThat(savedConnector.get().getProject()).isSameAs(project);
    assertThat(savedConnector.get().getCreatedBy()).isSameAs(creator);
    assertThat(savedConnector.get().getName()).isEqualTo("Mock Health Chatbot");
    assertThat(savedConnector.get().getUrl()).isEqualTo("http://localhost:8080/mock-chatbot/chat");
    assertThat(savedConnector.get().getHeaders())
        .containsEntry("Authorization", "Bearer {{secret:CHATBOT_API_TOKEN}}");
    assertThat(savedConnector.get().getAuthConfig())
        .containsEntry("tokenRef", "{{secret:CHATBOT_API_TOKEN}}");
    assertThat(savedConnector.get().getHeaders().toString()).doesNotContain("write-only-token-value");
    assertThat(savedConnector.get().getAuthConfig().toString()).doesNotContain("write-only-token-value");
    assertThat(savedConnector.get().getSecretRefs().getFirst())
        .containsEntry("secretKey", "CHATBOT_API_TOKEN")
        .containsEntry("maskedValue", "****alue");
    assertThat(savedConnector.get().getSecretRefs().toString()).doesNotContain("write-only-token-value");
    assertThat(response).isSameAs(mappedResponse);
  }

  @Test
  void createConnectorRejectsMissingProject() {
    User creator = new User();
    TargetApiConnectorServiceImpl service =
        new TargetApiConnectorServiceImpl(
            ignoredConnectorRepository(),
            projectRepository(Optional.empty(), new AtomicReference<>()),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            mapper(null));

    assertThatThrownBy(() -> service.createConnector(UUID.randomUUID(), request(), "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("PROJECT_NOT_FOUND");
  }

  @Test
  void listConnectorsLoadsOwnerScopedProjectAndReturnsPage() {
    User creator = new User();
    creator.setUsername("qc.demo@example.com");
    Project project = new Project();
    TargetApiConnector connector = new TargetApiConnector();
    connector.setProject(project);
    connector.setName("Mock Health Chatbot");
    AtomicReference<String> lookedUpUsername = new AtomicReference<>();
    AtomicReference<ProjectLookup> projectLookup = new AtomicReference<>();
    AtomicReference<Project> connectorProject = new AtomicReference<>();
    TargetApiConnectorListItemResponse itemResponse =
        new TargetApiConnectorListItemResponse(
            connector.getPublicId(),
            project.getPublicId(),
            "Mock Health Chatbot",
            HttpMethodType.POST,
            "http://localhost:8080/mock-chatbot/chat",
            "$.answer",
            false,
            true,
            null);
    TargetApiConnectorServiceImpl service =
        new TargetApiConnectorServiceImpl(
            connectorRepository(
                new AtomicReference<>(),
                new PageImpl<>(java.util.List.of(connector), PageRequest.of(0, 20), 1),
                connectorProject),
            projectRepository(Optional.of(project), projectLookup),
            userRepository(Optional.of(creator), lookedUpUsername),
            mapper(null, itemResponse));

    TargetApiConnectorPageResponse response =
        service.listConnectors(project.getPublicId(), PageRequest.of(0, 20), " QC.Demo@Example.COM ");

    assertThat(lookedUpUsername).hasValue("qc.demo@example.com");
    assertThat(projectLookup.get().publicId()).isEqualTo(project.getPublicId());
    assertThat(projectLookup.get().createdBy()).isSameAs(creator);
    assertThat(connectorProject).hasValue(project);
    assertThat(response.items()).containsExactly(itemResponse);
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalItems()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(1);
  }

  @Test
  void getConnectorLoadsCreatorScopedConnector() {
    User creator = new User();
    creator.setUsername("qc.demo@example.com");
    TargetApiConnector connector = new TargetApiConnector();
    AtomicReference<String> lookedUpUsername = new AtomicReference<>();
    AtomicReference<ConnectorLookup> connectorLookup = new AtomicReference<>();
    TargetApiConnectorResponse mappedResponse =
        new TargetApiConnectorResponse(
            connector.getPublicId(), null, "Mock Health Chatbot", null, HttpMethodType.POST, null,
            null, "http://localhost:8080/mock-chatbot/chat", Map.of(), Map.of(), Map.of(),
            BodyType.RAW_JSON, Map.of(), null, AuthType.NONE, Map.of(), java.util.List.of(),
            ResponseFormat.JSON, "$.answer", false, null, null, 60, 1, true, null, null);
    TargetApiConnectorServiceImpl service =
        new TargetApiConnectorServiceImpl(
            connectorRepository(
                new AtomicReference<>(), null, new AtomicReference<>(), Optional.of(connector), connectorLookup),
            projectRepository(Optional.empty(), new AtomicReference<>()),
            userRepository(Optional.of(creator), lookedUpUsername),
            mapper(mappedResponse));

    TargetApiConnectorResponse response =
        service.getConnector(connector.getPublicId(), " QC.Demo@Example.COM ");

    assertThat(lookedUpUsername).hasValue("qc.demo@example.com");
    assertThat(connectorLookup.get().publicId()).isEqualTo(connector.getPublicId());
    assertThat(connectorLookup.get().createdBy()).isSameAs(creator);
    assertThat(response).isSameAs(mappedResponse);
  }

  @Test
  void getConnectorRejectsMissingConnector() {
    User creator = new User();
    TargetApiConnectorServiceImpl service =
        new TargetApiConnectorServiceImpl(
            connectorRepository(
                new AtomicReference<>(), null, new AtomicReference<>(), Optional.empty(), new AtomicReference<>()),
            projectRepository(Optional.empty(), new AtomicReference<>()),
            userRepository(Optional.of(creator), new AtomicReference<>()),
            mapper(null));

    assertThatThrownBy(() -> service.getConnector(UUID.randomUUID(), "qc.demo@example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("TARGET_CONNECTOR_NOT_FOUND");
  }

  private CreateTargetApiConnectorRequest request() {
    return new CreateTargetApiConnectorRequest(
        "  Mock Health Chatbot  ",
        "Local mock chatbot for demo.",
        null,
        null,
        HttpMethodType.POST,
        "http://localhost:8080",
        "/mock-chatbot/chat",
        " http://localhost:8080/mock-chatbot/chat ",
        Map.of("Content-Type", "application/json", "Authorization", "Bearer write-only-token-value"),
        Map.of(),
        Map.of(),
        BodyType.RAW_JSON,
        Map.of("message", "{{question}}"),
        null,
        AuthType.BEARER,
        Map.of("tokenRef", "write-only-token-value"),
        Map.of("CHATBOT_API_TOKEN", "write-only-token-value"),
        ResponseFormat.JSON,
        "$.answer",
        false,
        null,
        null,
        60,
        1,
        true);
  }

  private TargetApiConnectorRepository connectorRepository(
      AtomicReference<TargetApiConnector> savedConnector) {
    return connectorRepository(savedConnector, null, new AtomicReference<>());
  }

  private TargetApiConnectorRepository connectorRepository(
      AtomicReference<TargetApiConnector> savedConnector,
      PageImpl<TargetApiConnector> connectorPage,
      AtomicReference<Project> connectorProject) {
    return connectorRepository(
        savedConnector, connectorPage, connectorProject, Optional.empty(), new AtomicReference<>());
  }

  private TargetApiConnectorRepository connectorRepository(
      AtomicReference<TargetApiConnector> savedConnector,
      PageImpl<TargetApiConnector> connectorPage,
      AtomicReference<Project> connectorProject,
      Optional<TargetApiConnector> foundConnector,
      AtomicReference<ConnectorLookup> connectorLookup) {
    return (TargetApiConnectorRepository)
        Proxy.newProxyInstance(
            TargetApiConnectorRepository.class.getClassLoader(),
            new Class<?>[] {TargetApiConnectorRepository.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "save" -> {
                  savedConnector.set((TargetApiConnector) args[0]);
                  yield args[0];
                }
                case "findByProject" -> {
                  connectorProject.set((Project) args[0]);
                  yield connectorPage;
                }
                case "findByPublicIdAndCreatedBy" -> {
                  connectorLookup.set(new ConnectorLookup((UUID) args[0], (User) args[1]));
                  yield foundConnector;
                }
                case "toString" -> "TargetApiConnectorRepositoryTestDouble";
                default -> throw new UnsupportedOperationException(method.getName());
              };
            });
  }

  private TargetApiConnectorRepository ignoredConnectorRepository() {
    return connectorRepository(new AtomicReference<>());
  }

  private ProjectRepository projectRepository(
      Optional<Project> project, AtomicReference<ProjectLookup> projectLookup) {
    return (ProjectRepository)
        Proxy.newProxyInstance(
            ProjectRepository.class.getClassLoader(),
            new Class<?>[] {ProjectRepository.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "findByPublicIdAndCreatedBy" -> {
                  projectLookup.set(new ProjectLookup((UUID) args[0], (User) args[1]));
                  yield project;
                }
                case "toString" -> "ProjectRepositoryTestDouble";
                default -> throw new UnsupportedOperationException(method.getName());
              };
            });
  }

  private UserRepository userRepository(Optional<User> user, AtomicReference<String> lookedUpUsername) {
    return (UserRepository)
        Proxy.newProxyInstance(
            UserRepository.class.getClassLoader(),
            new Class<?>[] {UserRepository.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "findByUsername" -> {
                  lookedUpUsername.set((String) args[0]);
                  yield user;
                }
                case "toString" -> "UserRepositoryTestDouble";
                default -> throw new UnsupportedOperationException(method.getName());
              };
            });
  }

  private TargetApiConnectorMapper mapper(TargetApiConnectorResponse response) {
    return mapper(response, null);
  }

  private TargetApiConnectorMapper mapper(
      TargetApiConnectorResponse response, TargetApiConnectorListItemResponse itemResponse) {
    return new TargetApiConnectorMapper() {
      @Override
      public TargetApiConnectorResponse toResponse(TargetApiConnector connector) {
        return response;
      }

      @Override
      public TargetApiConnectorListItemResponse toListItemResponse(TargetApiConnector connector) {
        return itemResponse;
      }
    };
  }

  private record ProjectLookup(UUID publicId, User createdBy) {}

  private record ConnectorLookup(UUID publicId, User createdBy) {}
}
