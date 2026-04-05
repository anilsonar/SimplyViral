package com.simplyviral.asset.service;

public interface AssetStorageService {
    /**
     * Stores raw bytes (image or audio) into the configured backend.
     * @param filename Desired file name with extension
     * @param contentType MIME type of the content
     * @param content Raw bytes
     * @return Publicly accessible URL or absolute file path to the asset
     */
    String storeAsset(String filename, String contentType, byte[] content);
}
