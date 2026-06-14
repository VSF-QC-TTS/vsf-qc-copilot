package me.nghlong3004.vqc.api.targetconnector.curl;

import java.util.Map;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;

/**
 * Result of parsing a raw cURL command into structured fields.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/14/2026
 */
public record CurlParseResult(
    HttpMethodType method,
    String url,
    Map<String, String> headers,
    String bodyRaw,
    Map<String, Object> bodyJson,
    boolean isJsonBody) {}
