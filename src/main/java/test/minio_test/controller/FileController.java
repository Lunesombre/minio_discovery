package test.minio_test.controller;

import io.minio.errors.MinioException;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import test.minio_test.service.FileService;


@RestController
@RequestMapping("/api/file")
public class FileController {

  private final FileService fileService;
  private final Logger LOG = LoggerFactory.getLogger(FileController.class);

  public FileController(FileService fileService) {
    this.fileService = fileService;
  }

  @PostMapping("/upload")
  public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
      @RequestParam(value = "newFileName", required = false) String newFileName) {
    try {
      // modifier le nom du fichier si un nouveau nom était dans la requête
      String fileName = newFileName != null ? newFileName : file.getOriginalFilename();

      // vérifier si le fichier existe déjà dans le bucket
      if (fileService.fileExists("minio-test", fileName)) {
        //s'il existe, le supprimer (permet d'écraser en ré-uploadant un fichier, à voir si on veut ce fonctionnement)
        fileService.removeFile("minio-test", fileName);
      }
      fileService.uploadFile("minio-test", fileName, file.getInputStream());
      return ResponseEntity.ok("File uploaded successfully!");
    } catch (Exception e) {
      LOG.error("Failed upload due to: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file!");
    }
  }


  /**
   * Télécharge le fichier spécifié à partir du service de stockage Minio.
   * <br>
   * Retourne une ResponseEntity{@literal <?> } parce que c'est soit un fichier ({@literal ResponseEntity<Resource>}), soit la 404 avec le message
   * ({@literal ResponseEntity<String> }), mais avec Resource, je ne pouvais renvoyer qu'une 404 sans message via ResponseEntity.notFound().build();
   * <br>
   *
   * @param fileName Le nom du fichier à télécharger.
   * @return ResponseEntity avec le contenu du fichier sous forme de Resource si le fichier existe, sinon un message d'erreur.
   */
  @GetMapping("/download")
  public ResponseEntity<?> downloadFile(@RequestParam("fileName") String fileName) {
    try {
      // vérifier si le fichier existe déjà dans le bucket
      if (fileService.fileExists("minio-test", fileName)) {
        InputStream inputStream = fileService.downloadFile("minio-test", fileName);
        ByteArrayResource resource = new ByteArrayResource(IOUtils.toByteArray(inputStream));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .body(resource);
      } else {
        String errorMessage = "Le fichier '" + fileName + "' n'existe pas.";
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
      }
    } catch (Exception e) {
      LOG.error("Failed upload due to: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/delete/{fileName}")
  public ResponseEntity<String> deleteFile(@PathVariable("fileName") String fileName) {
    try {
      // vérifier si le fichier existe déjà dans le bucket
      if (!fileService.fileExists("minio-test", fileName)) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Le fichier '" + fileName + "' n'existe pas.");
      }

      // Appel à la méthode de suppression du service
      fileService.removeFile("minio-test", fileName);
      return ResponseEntity.ok("Fichier supprimé avec succès.");

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Erreur lors de la suppression du fichier: " + e.getMessage());
    }
  }

  @GetMapping("/verify/{filename}")
  public ResponseEntity<String> findFile(@PathVariable("filename") String fileName) {
    try {
      // vérifier si le fichier existe déjà dans le bucket
      if (!fileService.fileExists("minio-test", fileName)) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Le fichier '" + fileName + "' n'existe pas.");
      }
      return ResponseEntity.ok("Le fichier '" + fileName + "' est bien stocké ici.");

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Erreur lors de la suppression du fichier: " + e.getMessage());
    }
  }

  @GetMapping("listAllFiles/{bucketName}")
  public ResponseEntity<String> findAllFiles(@PathVariable("bucketName") String bucketName) throws MinioException {
    List<String> filesList = fileService.getAllFiles(bucketName);
    return ResponseEntity.ok("Fichiers présents dans le bucket : " + filesList.toString());
  }
}
