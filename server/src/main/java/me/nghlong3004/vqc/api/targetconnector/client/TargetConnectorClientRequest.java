package me.nghlong3004.vqc.api.targetconnector.client;

import java.util.Map;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public record TargetConnectorClientRequest(
    HttpMethodType method, String url, Map<String, Object> headers, Object body) {}
