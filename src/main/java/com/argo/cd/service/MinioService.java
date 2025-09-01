package com.argo.cd.service;

import com.argo.cd.config.MinioConfig;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {
  private final MinioClient minioClient;
  private final MinioConfig minioConfig;

  @Value("${minio.bucket-name}")
  private String bucketName;

  /** Initialize bucket on application startup if it doesn't exist. */
  @PostConstruct
  public void initializeBucket() {
    try {
      createBucketIfNotExists();
      log.info("Bucket {} is ready.", bucketName);
    } catch (Exception e) {
      log.error("Failed to initialize bucket {}", bucketName, e);
      throw new RuntimeException("Bucket initialization failed", e);
    }
  }

  /** Check if bucket exists, create it if it doesn't. */
  private void createBucketIfNotExists() throws Exception {
    boolean bucketExists =
        minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    if (!bucketExists) {
      minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      log.info("Created bucket {}", bucketName);
    } else {
      log.debug("Bucket {} already exists.", bucketName);
    }
  }

  public String upload(MultipartFile file) {
    final String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
    try (final var inputStream = file.getInputStream()) {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                  inputStream, file.getSize(), -1)
              .contentType(file.getContentType())
              .build());

      return this.generatePresignedUrl(fileName);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return null;
  }

  public String getPresignedUrl(String fileName) {
    try {
      String url = String.format("%s/%s/%s", minioConfig.getUrl(), bucketName, fileName);
      log.debug("Generated public URL for {}: {}", fileName, url);
      return url;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String generatePresignedUrl(String fileName) {
    try {
      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(bucketName)
              .object(fileName)
              .expiry(3600)
              .build());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
