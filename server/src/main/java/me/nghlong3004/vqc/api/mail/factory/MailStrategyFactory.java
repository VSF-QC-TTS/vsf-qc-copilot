package me.nghlong3004.vqc.api.mail.factory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import me.nghlong3004.vqc.api.mail.model.MailType;
import me.nghlong3004.vqc.api.mail.strategy.MailStrategy;
import org.springframework.stereotype.Component;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@Component
public class MailStrategyFactory {

  private final Map<MailType, MailStrategy> strategies;

  public MailStrategyFactory(List<MailStrategy> strategies) {
    this.strategies = new EnumMap<>(MailType.class);
    for (MailStrategy strategy : strategies) {
      this.strategies.put(strategy.type(), strategy);
    }
  }

  public MailStrategy get(MailType type) {
    MailStrategy strategy = strategies.get(type);
    if (strategy == null) {
      throw new IllegalArgumentException("Unsupported mail type: " + type);
    }
    return strategy;
  }
}
