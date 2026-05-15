package com.lab2.demo.service;

import com.lab2.demo.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Service
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public String uploadFile(String objectKey,
                             byte[] content,
                             String contentType) {
        try {
            ensureBucketExists();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );

            return objectKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload file to MinIO", ex);
        }
    }

    public String generatePresignedUrl(String objectKey) {
        try {
            int expirationMinutes = properties.getPresignedUrlExpirationMinutes() != null
                    ? properties.getPresignedUrlExpirationMinutes()
                    : 10;

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .expiry(expirationMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate pre-signed URL", ex);
        }
    }

    public Instant calculateExpirationTime() {
        int expirationMinutes = properties.getPresignedUrlExpirationMinutes() != null
                ? properties.getPresignedUrlExpirationMinutes()
                : 10;

        return Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(properties.getBucket())
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(properties.getBucket())
                            .build()
            );
        }
    }
}