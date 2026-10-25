package com.payroll.aj.infrastructure.storage;

import java.time.Duration;

/**
 * Generic abstraction for Object Storage.
 */
public interface StorageService {
    
    /**
     * Uploads a PDF to the configured object storage.
     *
     * @param objectPath  the exact path/key of the object
     * @param content     the file content
     * @param contentType the MIME type
     */
    void uploadPdf(String objectPath, byte[] content, String contentType);

    /**
     * Generates a pre-signed URL for accessing a stored object.
     *
     * @param objectPath the exact path/key of the object
     * @param expiry     how long the URL should remain valid
     * @return the pre-signed URL
     */
    String generatePresignedUrl(String objectPath, Duration expiry);

    /**
     * Downloads an object from storage as a byte array.
     *
     * @param objectPath the exact path/key of the object
     * @return the file bytes
     */
    byte[] downloadPdf(String objectPath);
}
