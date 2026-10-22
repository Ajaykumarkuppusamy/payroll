package com.payroll.aj.infrastructure.minio;

import com.payroll.aj.infrastructure.storage.StorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket", e);
            throw new RuntimeException("MinIO initialization failed", e);
        }
    }

    @Override
    public void uploadPdf(String objectPath, byte[] content, String contentType) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectPath)
                    .stream(bais, content.length, -1)
                    .contentType(contentType)
                    .build());
            log.debug("Successfully uploaded to MinIO: {}/{}", bucketName, objectPath);
        } catch (Exception e) {
            log.error("Failed to upload PDF to MinIO path: {}", objectPath, e);
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    @Override
    public String generatePresignedUrl(String objectPath, Duration expiry) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectPath)
                    .expiry(Math.toIntExact(expiry.toSeconds()), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for path: {}", objectPath, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
}
