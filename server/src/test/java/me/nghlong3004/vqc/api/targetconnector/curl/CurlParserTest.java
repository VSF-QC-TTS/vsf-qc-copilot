package me.nghlong3004.vqc.api.targetconnector.curl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import org.junit.jupiter.api.Test;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/14/2026
 */
class CurlParserTest {

  private final CurlParser parser = new CurlParser(new ObjectMapper());

  @Test
  void parsesFullCurlWithPostJsonBodyAndMultipleHeaders() {
    String curl =
        """
        curl --location 'https://d2epqhv6m96o2l.cloudfront.net/v1/responses' \
        --header 'Authorization: Bearer token' \
        --header 'user-id: haint119-sit-2' \
        --header 'user-name: Hai Nguyen' \
        --header 'Content-Type: application/json' \
        --data '{
          "model": "ToanGPT SIT",
          "input": "giá vàng hôm nay",
          "stream": true,
          "thinking_level": "fast",
          "debug": true
        }'
        """;

    CurlParseResult result = parser.parse(curl);

    assertThat(result.method()).isEqualTo(HttpMethodType.POST);
    assertThat(result.url()).isEqualTo("https://d2epqhv6m96o2l.cloudfront.net/v1/responses");
    assertThat(result.headers())
        .containsEntry("Authorization", "Bearer token")
        .containsEntry("user-id", "haint119-sit-2")
        .containsEntry("user-name", "Hai Nguyen")
        .containsEntry("Content-Type", "application/json");
    assertThat(result.isJsonBody()).isTrue();
    assertThat(result.bodyJson())
        .containsEntry("model", "ToanGPT SIT")
        .containsEntry("input", "giá vàng hôm nay")
        .containsEntry("stream", true)
        .containsEntry("thinking_level", "fast")
        .containsEntry("debug", true);
  }

  @Test
  void parsesGetRequestWithoutBody() {
    String curl = "curl 'https://api.example.com/health'";
    CurlParseResult result = parser.parse(curl);

    assertThat(result.method()).isEqualTo(HttpMethodType.GET);
    assertThat(result.url()).isEqualTo("https://api.example.com/health");
    assertThat(result.headers()).isEmpty();
    assertThat(result.bodyRaw()).isNull();
    assertThat(result.isJsonBody()).isFalse();
  }

  @Test
  void parsesExplicitMethodOverride() {
    String curl = "curl -X PUT 'https://api.example.com/users/1' -d '{\"name\":\"John\"}'";
    CurlParseResult result = parser.parse(curl);

    assertThat(result.method()).isEqualTo(HttpMethodType.PUT);
    assertThat(result.url()).isEqualTo("https://api.example.com/users/1");
    assertThat(result.isJsonBody()).isTrue();
  }

  @Test
  void defaultsToPostWhenBodyPresent() {
    String curl = "curl 'https://api.example.com/chat' -d '{\"msg\":\"hi\"}'";
    CurlParseResult result = parser.parse(curl);

    assertThat(result.method()).isEqualTo(HttpMethodType.POST);
  }

  @Test
  void parsesDataRawFlag() {
    String curl =
        "curl --request POST 'https://api.example.com/chat' --data-raw '{\"msg\":\"hello\"}'";
    CurlParseResult result = parser.parse(curl);

    assertThat(result.method()).isEqualTo(HttpMethodType.POST);
    assertThat(result.isJsonBody()).isTrue();
    assertThat(result.bodyJson()).containsEntry("msg", "hello");
  }

  @Test
  void handlesNonJsonBody() {
    String curl = "curl -X POST 'https://api.example.com/data' -d 'key=value&foo=bar'";
    CurlParseResult result = parser.parse(curl);

    assertThat(result.isJsonBody()).isFalse();
    assertThat(result.bodyRaw()).isEqualTo("key=value&foo=bar");
    assertThat(result.bodyJson()).isNull();
  }

  @Test
  void rejectsNullInput() {
    assertThatThrownBy(() -> parser.parse(null))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("CURL_PARSE_ERROR");
  }

  @Test
  void rejectsBlankInput() {
    assertThatThrownBy(() -> parser.parse("   "))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("CURL_PARSE_ERROR");
  }

  @Test
  void rejectsInputNotStartingWithCurl() {
    assertThatThrownBy(() -> parser.parse("wget https://example.com"))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("CURL_PARSE_ERROR");
  }

  @Test
  void rejectsCurlWithoutUrl() {
    assertThatThrownBy(() -> parser.parse("curl --header 'Content-Type: application/json'"))
        .isInstanceOf(ResourceException.class)
        .extracting("response.code")
        .isEqualTo("CURL_PARSE_ERROR");
  }

  @Test
  void parsesDoubleQuotedStrings() {
    String curl = "curl -X POST \"https://api.example.com/chat\" -d \"{\\\"msg\\\":\\\"hi\\\"}\"";
    CurlParseResult result = parser.parse(curl);

    assertThat(result.url()).isEqualTo("https://api.example.com/chat");
    assertThat(result.isJsonBody()).isTrue();
  }

  @Test
  void ignoresLocationAndCompressedFlags() {
    String curl =
        "curl --location --compressed -H 'Accept: application/json' 'https://api.example.com/data'";
    CurlParseResult result = parser.parse(curl);

    assertThat(result.method()).isEqualTo(HttpMethodType.GET);
    assertThat(result.url()).isEqualTo("https://api.example.com/data");
    assertThat(result.headers()).containsEntry("Accept", "application/json");
  }

  @Test
  void parsesUrlFlag() {
    String curl = "curl --url 'https://api.example.com/v1' -H 'Accept: text/plain'";
    CurlParseResult result = parser.parse(curl);

    assertThat(result.url()).isEqualTo("https://api.example.com/v1");
  }
}
