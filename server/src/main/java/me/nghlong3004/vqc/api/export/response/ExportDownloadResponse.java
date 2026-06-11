package me.nghlong3004.vqc.api.export.response;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public record ExportDownloadResponse(String fileName, MediaType mediaType, Resource resource) {}
