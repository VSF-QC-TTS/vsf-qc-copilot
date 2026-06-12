package me.nghlong3004.vqc.api.storage.model;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public record StoredObject(String provider, String key, String contentType, long sizeBytes) {}
