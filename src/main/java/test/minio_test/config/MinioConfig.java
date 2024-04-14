package test.minio_test.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

  @Value("${minio.url}")
  private String minioUrl;

  @Value("${minio.access-key}")
  private String accessKey;

  @Value("${minio.secret-key}")
  private String secretKey;

  @Value("${minio.bucket-name}")
  private String bucketName;

  @Bean
  public MinioClient minioClient() {
    MinioClient minioClient = MinioClient.builder()
        .endpoint(minioUrl)
        .credentials(accessKey, secretKey)
        .build();

    // Vérifier si le bucket existe, sinon le créer
    try {
      boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
      if (!bucketExists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        LoggerFactory.getLogger(MinioConfig.class).info("Bucket created successfully.");
      } else {
        LoggerFactory.getLogger(MinioConfig.class).info("Bucket already exists.");
      }
    } catch (Exception e) {
      LoggerFactory.getLogger(MinioConfig.class).warn("Error occurred while creating bucket: " + e.getMessage());
    }

    return minioClient;
  }
}
