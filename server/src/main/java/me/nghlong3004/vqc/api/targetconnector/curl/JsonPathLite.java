package me.nghlong3004.vqc.api.targetconnector.curl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Minimal JSONPath subset for connector response extraction: $.field[0].nested. */
public final class JsonPathLite {

  private JsonPathLite() {}

  public static Object extract(Object root, String selector) {
    if ("$".equals(selector)) {
      return root;
    }
    Object current = root;
    for (Segment segment : parse(selector)) {
      if (segment.name() != null) {
        if (!(current instanceof Map<?, ?> map)) {
          return null;
        }
        current = map.get(segment.name());
      }
      if (segment.index() != null) {
        if (!(current instanceof List<?> list) || segment.index() >= list.size()) {
          return null;
        }
        current = list.get(segment.index());
      }
      if (current == null) {
        return null;
      }
    }
    return current;
  }

  public static String extractString(Object root, String selector) {
    Object value = extract(root, selector);
    if (value == null || value instanceof Map<?, ?> || value instanceof List<?>) {
      return null;
    }
    return value.toString();
  }

  public static String toJavascriptExpression(String selector) {
    if ("$".equals(selector)) {
      return "json";
    }
    StringBuilder expression = new StringBuilder("json");
    for (Segment segment : parse(selector)) {
      if (segment.name() != null) {
        expression.append("?.").append(segment.name());
      }
      if (segment.index() != null) {
        expression.append("?.[").append(segment.index()).append(']');
      }
    }
    return expression.toString();
  }

  private static List<Segment> parse(String selector) {
    if (selector == null || selector.isBlank() || !selector.startsWith("$")) {
      throw new IllegalArgumentException("JSONPath selector must start with $");
    }
    List<Segment> segments = new ArrayList<>();
    int i = 1;
    while (i < selector.length()) {
      if (selector.charAt(i) != '.') {
        throw new IllegalArgumentException("Unsupported JSONPath selector: " + selector);
      }
      i++;
      int start = i;
      while (i < selector.length() && isNameChar(selector.charAt(i))) {
        i++;
      }
      if (start == i) {
        throw new IllegalArgumentException("Unsupported JSONPath selector: " + selector);
      }
      String name = selector.substring(start, i);
      Integer index = null;
      if (i < selector.length() && selector.charAt(i) == '[') {
        int end = selector.indexOf(']', i);
        if (end < 0) {
          throw new IllegalArgumentException("Unsupported JSONPath selector: " + selector);
        }
        String indexText = selector.substring(i + 1, end);
        if (indexText.isBlank() || !indexText.chars().allMatch(Character::isDigit)) {
          throw new IllegalArgumentException("Unsupported JSONPath selector: " + selector);
        }
        index = Integer.parseInt(indexText);
        i = end + 1;
      }
      segments.add(new Segment(name, index));
    }
    return segments;
  }

  private static boolean isNameChar(char value) {
    return (value >= 'A' && value <= 'Z')
        || (value >= 'a' && value <= 'z')
        || (value >= '0' && value <= '9')
        || value == '_';
  }

  private record Segment(String name, Integer index) {}
}
