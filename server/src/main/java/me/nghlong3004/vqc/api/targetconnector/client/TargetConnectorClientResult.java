package me.nghlong3004.vqc.api.targetconnector.client;

import java.util.Map;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
public record TargetConnectorClientResult(Map<String, Object> rawResponse, long latencyMs) {}
