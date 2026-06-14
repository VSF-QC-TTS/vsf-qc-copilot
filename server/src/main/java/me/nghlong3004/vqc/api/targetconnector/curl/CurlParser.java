package me.nghlong3004.vqc.api.targetconnector.curl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.exception.ErrorCode;
import me.nghlong3004.vqc.api.exception.ResourceException;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;
import org.springframework.stereotype.Component;

/**
 * Parses a raw cURL command string into structured {@link CurlParseResult}.
 *
 * <p>Supports common cURL flags: {@code --request/-X}, {@code --header/-H}, {@code --data/-d/
 * --data-raw}, {@code --url}, and ignores non-relevant flags like {@code --location},
 * {@code --compressed}.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/14/2026
 */
@Component
@RequiredArgsConstructor
public class CurlParser {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final ObjectMapper objectMapper;

  /**
   * Parses a raw cURL command into structured fields.
   *
   * @param rawCurl the raw cURL command string
   * @return parsed result with method, url, headers, and body
   * @throws ResourceException with {@code CURL_PARSE_ERROR} if the input is invalid
   */
  public CurlParseResult parse(String rawCurl) {
    if (rawCurl == null || rawCurl.isBlank()) {
      throw new ResourceException(ErrorCode.CURL_PARSE_ERROR);
    }

    String normalized = normalize(rawCurl);
    List<String> tokens = tokenize(normalized);

    if (tokens.isEmpty() || !tokens.getFirst().equalsIgnoreCase("curl")) {
      throw new ResourceException(ErrorCode.CURL_PARSE_ERROR);
    }

    String method = null;
    String url = null;
    Map<String, String> headers = new LinkedHashMap<>();
    StringBuilder bodyBuilder = new StringBuilder();

    int i = 1;
    while (i < tokens.size()) {
      String token = tokens.get(i);
      switch (token) {
        case "--request", "-X" -> {
          method = requireNext(tokens, i, "method");
          i += 2;
        }
        case "--header", "-H" -> {
          String headerValue = requireNext(tokens, i, "header");
          parseHeader(headerValue, headers);
          i += 2;
        }
        case "--data", "--data-raw", "--data-binary", "-d" -> {
          String data = requireNext(tokens, i, "data");
          if (!bodyBuilder.isEmpty()) {
            bodyBuilder.append("&");
          }
          bodyBuilder.append(data);
          i += 2;
        }
        case "--url" -> {
          url = requireNext(tokens, i, "url");
          i += 2;
        }
        case "--location", "-L", "--compressed", "--insecure", "-k", "-s", "--silent", "-S",
            "--show-error", "-v", "--verbose", "--globoff", "-g" -> i++;
        default -> {
          if (token.startsWith("-")) {
            // Unknown flag with possible value — skip flag + value
            if (i + 1 < tokens.size() && !tokens.get(i + 1).startsWith("-")) {
              i += 2;
            } else {
              i++;
            }
          } else if (url == null) {
            url = token;
            i++;
          } else {
            i++;
          }
        }
      }
    }

    if (url == null || url.isBlank()) {
      throw new ResourceException(ErrorCode.CURL_PARSE_ERROR);
    }

    String bodyRaw = bodyBuilder.isEmpty() ? null : bodyBuilder.toString();
    HttpMethodType httpMethod = resolveMethod(method, bodyRaw);
    Map<String, Object> bodyJson = tryParseJson(bodyRaw);
    boolean isJsonBody = bodyJson != null;

    return new CurlParseResult(httpMethod, url, headers, bodyRaw, bodyJson, isJsonBody);
  }

  private String normalize(String rawCurl) {
    return rawCurl
        .replace("\\\n", " ")
        .replace("\\\r\n", " ")
        .replace("\\\r", " ")
        .replaceAll("\\\\\\s*\n", " ")
        .trim();
  }

  /**
   * Tokenizes the normalized cURL string, respecting single and double quoted strings.
   */
  List<String> tokenize(String input) {
    List<String> tokens = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      if (inSingleQuote) {
        if (c == '\'') {
          inSingleQuote = false;
        } else {
          current.append(c);
        }
      } else if (inDoubleQuote) {
        if (c == '\\' && i + 1 < input.length()) {
          char next = input.charAt(i + 1);
          if (next == '"' || next == '\\') {
            current.append(next);
            i++;
          } else {
            current.append(c);
          }
        } else if (c == '"') {
          inDoubleQuote = false;
        } else {
          current.append(c);
        }
      } else {
        if (c == '\'') {
          inSingleQuote = true;
        } else if (c == '"') {
          inDoubleQuote = true;
        } else if (Character.isWhitespace(c)) {
          if (!current.isEmpty()) {
            tokens.add(current.toString());
            current.setLength(0);
          }
        } else {
          current.append(c);
        }
      }
    }

    if (!current.isEmpty()) {
      tokens.add(current.toString());
    }

    return tokens;
  }

  private void parseHeader(String headerValue, Map<String, String> headers) {
    int colonIndex = headerValue.indexOf(':');
    if (colonIndex <= 0) {
      return;
    }
    String name = headerValue.substring(0, colonIndex).trim();
    String value = headerValue.substring(colonIndex + 1).trim();
    headers.put(name, value);
  }

  private String requireNext(List<String> tokens, int currentIndex, String fieldName) {
    if (currentIndex + 1 >= tokens.size()) {
      throw new ResourceException(ErrorCode.CURL_PARSE_ERROR);
    }
    return tokens.get(currentIndex + 1);
  }

  private HttpMethodType resolveMethod(String explicit, String bodyRaw) {
    if (explicit != null) {
      try {
        return HttpMethodType.valueOf(explicit.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new ResourceException(ErrorCode.CURL_PARSE_ERROR);
      }
    }
    return bodyRaw != null ? HttpMethodType.POST : HttpMethodType.GET;
  }

  private Map<String, Object> tryParseJson(String bodyRaw) {
    if (bodyRaw == null || bodyRaw.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(bodyRaw.trim(), MAP_TYPE);
    } catch (Exception e) {
      return null;
    }
  }
}
