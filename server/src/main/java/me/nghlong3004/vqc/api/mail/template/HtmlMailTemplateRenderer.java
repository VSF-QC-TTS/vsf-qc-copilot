package me.nghlong3004.vqc.api.mail.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Component
public class HtmlMailTemplateRenderer {

  private static final String CLASSPATH_PREFIX = "classpath:";

  private final ResourceLoader resourceLoader;

  public HtmlMailTemplateRenderer(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public String render(String templatePath, Map<String, ?> model) {
    String rendered = loadTemplate(templatePath);
    for (var entry : model.entrySet()) {
      rendered =
          rendered.replace(
              "{{" + entry.getKey() + "}}", escapeHtml(String.valueOf(entry.getValue())));
    }
    return rendered;
  }

  private String loadTemplate(String templatePath) {
    Resource resource = resourceLoader.getResource(CLASSPATH_PREFIX + templatePath);
    try (var inputStream = resource.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load mail template: " + templatePath, ex);
    }
  }

  private String escapeHtml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
