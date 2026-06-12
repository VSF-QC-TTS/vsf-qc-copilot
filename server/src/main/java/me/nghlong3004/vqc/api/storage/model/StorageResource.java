package me.nghlong3004.vqc.api.storage.model;

import org.springframework.core.io.Resource;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public record StorageResource(String key, Resource resource) {}
