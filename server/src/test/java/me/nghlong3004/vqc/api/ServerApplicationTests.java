package me.nghlong3004.vqc.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
    properties = {
      "JWT_SECRET_KEY=0123456789abcdef0123456789abcdef",
      "VQC_SECRET_ENCRYPTION_KEY=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      "SERVER_BASE_URL=http://localhost:8080",
      "WEB_BASE_URL=http://localhost:5173",
      "GOOGLE_CLIENT_ID=test-google-client-id",
      "GOOGLE_CLIENT_SECRET=test-google-client-secret",
      "GITHUB_CLIENT_ID=test-github-client-id",
      "GITHUB_CLIENT_SECRET=test-github-client-secret",
      "GEMINI_API_KEY=test-gemini-api-key"
    })
class ServerApplicationTests {

  @Test
  void contextLoads() {}
}
