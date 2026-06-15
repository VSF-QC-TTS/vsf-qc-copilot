package me.nghlong3004.vqc.api.targetconnector.curl;

import java.util.Map;

public record ConnectorInferenceResult(
    Map<String, Object> bodyTemplate,
    String bodyTemplateText,
    String responseSelector,
    Map<String, Object> responseSchema) {}
