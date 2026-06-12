package me.nghlong3004.vqc.api.storage.service;

import me.nghlong3004.vqc.api.storage.model.StoreObjectCommand;
import me.nghlong3004.vqc.api.storage.model.StorageResource;
import me.nghlong3004.vqc.api.storage.model.StoredObject;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
public interface ObjectStorageService {

  StoredObject store(StoreObjectCommand command);

  StorageResource load(String key);
}
