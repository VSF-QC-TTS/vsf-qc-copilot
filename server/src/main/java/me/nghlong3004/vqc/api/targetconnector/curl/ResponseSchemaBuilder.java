package me.nghlong3004.vqc.api.targetconnector.curl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResponseSchemaBuilder {

  private ResponseSchemaBuilder() {}

  @SuppressWarnings("unchecked")
  public static Map<String, Object> schema(Map<String, Object> rawResponse) {
    Object schema = shape(rawResponse);
    return schema instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of("$", schema);
  }

  private static Object shape(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> shaped = new LinkedHashMap<>();
      map.forEach((key, item) -> shaped.put(String.valueOf(key), shape(item)));
      return shaped;
    }
    if (value instanceof List<?> list) {
      if (list.isEmpty()) {
        return List.of();
      }
      return List.of(shape(list.getFirst()));
    }
    if (value instanceof Number) {
      return "number";
    }
    if (value instanceof Boolean) {
      return "boolean";
    }
    return "string";
  }
}
