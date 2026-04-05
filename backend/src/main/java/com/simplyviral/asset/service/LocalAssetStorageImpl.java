package com.simplyviral.asset.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@Primary
@Service
public class LocalAssetStorageImpl implements AssetStorageService {

    private final Path storageDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "simplyviral_assets");

    public LocalAssetStorageImpl() {
        try {
            Files.createDirectories(storageDirectory);
            log.info("Local asset storage initialized at: {}", storageDirectory.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create local asset storage directory", e);
        }
    }

    @Override
    public String storeAsset(String filename, String contentType, byte[] content) {
        try {
            String safeFilename = UUID.randomUUID() + "_" + filename;
            Path filePath = storageDirectory.resolve(safeFilename);
            Files.write(filePath, content, StandardOpenOption.CREATE_NEW);
            
            // Returns absolute URI mapping for local dev previews
            return filePath.toUri().toString();
        } catch (IOException e) {
            log.error("Failed to write asset to disk", e);
            throw new RuntimeException("Asset storage failed", e);
        }
    }
}
