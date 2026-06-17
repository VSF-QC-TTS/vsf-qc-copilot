package me.nghlong3004.vqc.api.mail.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
class HtmlMailTemplateRendererTest {

  private final HtmlMailTemplateRenderer renderer =
      new HtmlMailTemplateRenderer(new DefaultResourceLoader());

  @Test
  void renderEscapesModelValuesAndKeepsEmailSafeMarkup() {
    String html =
        renderer.render(
            "templates/mail/email-verification.html",
            Map.of(
                "appName", "VSF <QC>",
                "greeting", "Hi Long <script>",
                "actionUrl", "https://example.test/?a=1&b=2",
                "preheader", "Ready & waiting",
                "title", "Title",
                "body", "Body",
                "buttonText", "Button",
                "expiryNote", "Expiry",
                "automatedNote", "Automated"));

    assertThat(html).contains("VSF &lt;QC&gt;");
    assertThat(html).contains("Hi Long &lt;script&gt;");
    assertThat(html).contains("https://example.test/?a=1&amp;b=2");
    assertThat(html).contains("role=\"presentation\"");
    assertThat(html).contains("display:none");
  }
}
