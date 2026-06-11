package me.nghlong3004.vqc.api.review.enums;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public enum QcStatus {
  NOT_REVIEWED,
  PASS,
  FAIL,
  NEED_FIX,
  IGNORED;

  /**
   * Returns whether this status can be persisted through review write APIs.
   *
   * @return true for writable QC decisions
   */
  public boolean isWritable() {
    return this != NOT_REVIEWED;
  }
}
