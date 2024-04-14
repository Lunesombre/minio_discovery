package test.minio_test.service;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;


@Service
public class FileService {

  private final MinioClient minioClient;


  public FileService(MinioClient minioClient) {
    this.minioClient = minioClient;
  }

  public void uploadFile(String bucketName, String objectName, InputStream inputStream) throws Exception {
    try {
      minioClient.putObject(PutObjectArgs.builder()
          .bucket(bucketName)
          .object(objectName)
          // .stream attend 3 param : le stream, sa taille (on ne peut pas la connaitre sur un input stream, donc utiliser inputStream.available est un peu un pis-aller), et la taille des parties, -1 laisse minio gérer ce paramètre).
          .stream(inputStream, inputStream.available(), -1)
          .build());
    } catch (MinioException e) {
      e.printStackTrace();
      throw new Exception("Failed to upload file to Minio", e);
    }
  }

  public InputStream downloadFile(String bucketName, String objectName) throws Exception {
    try {
      return minioClient.getObject(
          GetObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .build());
    } catch (MinioException e) {
      e.printStackTrace();
      throw new Exception("Failed to download file from Minio", e);
    }
  }

  public boolean fileExists(String bucketName, String fileName) {
    try {
      // Vérifier si le fichier existe dans le bucket
      StatObjectResponse statObjectResponse = minioClient.statObject(StatObjectArgs.builder()
          .bucket(bucketName)
          .object(fileName)
          .build());
      return statObjectResponse != null;
    } catch (Exception e) {
      //Si le fichier n'existe pas ou s'il y a une erreur de connexion
      e.printStackTrace();
      return false;
    }
  }

  public void removeFile(String bucketName, String fileName) {
    try {
      // Supprimer le fichier du bucket
      minioClient.removeObject(RemoveObjectArgs.builder()
          .bucket(bucketName)
          .object(fileName)
          .build());
    } catch (Exception e) {
      // si le fichier n'existe pas ou s'il y a une erreur de connexion
      e.printStackTrace();
    }
  }

  public List<String> getAllFiles(String bucketName) throws MinioException {
    List<String> fileNames = new ArrayList<>();
    try {
      Iterable<Result<Item>> results = minioClient.listObjects(
          ListObjectsArgs.builder().bucket(bucketName).build());

      for (Result<Item> result : results) {
        Item item = result.get();
        fileNames.add(item.objectName());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return fileNames;
  }
}
