package me.nghlong3004.vqc.api.storage.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import me.nghlong3004.vqc.api.config.StorageProperties;
import me.nghlong3004.vqc.api.storage.model.StoreObjectCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
class LocalObjectStorageServiceTest {

  @Test
  void storeWritesObjectAndLoadReturnsResource(@TempDir Path tempDir) throws Exception {
    LocalObjectStorageService service = service(tempDir);
    StoreObjectCommand command =
        new StoreObjectCommand("exports/export-1/result.json", "result.json", "application/json", "[]".getBytes());

    var stored = service.store(command);
    var loaded = service.load(command.key());

    assertThat(stored.provider()).isEqualTo("LOCAL");
    assertThat(stored.key()).isEqualTo(command.key());
    assertThat(stored.contentType()).isEqualTo("application/json");
    assertThat(stored.sizeBytes()).isEqualTo(2L);
    assertThat(Files.readString(tempDir.resolve(command.key()))).isEqualTo("[]");
    assertThat(loaded.resource().exists()).isTrue();
  }

  @Test
  void loadRejectsMissingObject(@TempDir Path tempDir) {
    LocalObjectStorageService service = service(tempDir);

    assertThatThrownBy(() -> service.load("exports/missing.json"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void storeRejectsEscapedKey(@TempDir Path tempDir) {
    LocalObjectStorageService service = service(tempDir);

    assertThatThrownBy(
            () ->
                service.store(
                    new StoreObjectCommand("../escape.json", "escape.json", "application/json", "{}".getBytes())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to store local object");
  }

  private LocalObjectStorageService service(Path tempDir) {
    StorageProperties properties = new StorageProperties();
    properties.getLocal().setBaseDir(tempDir.toString());
    return new LocalObjectStorageService(properties);
  }
}
