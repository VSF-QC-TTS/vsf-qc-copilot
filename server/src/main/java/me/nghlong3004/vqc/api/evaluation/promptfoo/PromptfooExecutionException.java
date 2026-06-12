package me.nghlong3004.vqc.api.evaluation.promptfoo;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/12/2026
 */
public class PromptfooExecutionException extends RuntimeException {

  public PromptfooExecutionException(String message) {
    super(message);
  }

  public PromptfooExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
