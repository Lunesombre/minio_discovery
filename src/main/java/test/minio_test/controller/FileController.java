package test.minio_test.controller;

import io.minio.errors.MinioException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import test.minio_test.persistence.UploadedFilesEntity;
import test.minio_test.persistence.UploadedFilesRepository;
import test.minio_test.service.FileService;


// En vrai, il faudrait utiliser une lib comme MinIO Event Notification pour récupérer les events de ce qu'il se passe vraiment chez MinIo,
// mais dans mon usage, ce sera suffisant d'ajouter une ligne en db pour valider les actions.
@RestController
@RequestMapping("/api/file")
public class FileController {

  private final FileService fileService;
  private final UploadedFilesRepository uploadedFilesRepository;
  private final Logger LOG = LoggerFactory.getLogger(FileController.class);

  public FileController(FileService fileService, UploadedFilesRepository uploadedFilesRepository) {
    this.fileService = fileService;
    this.uploadedFilesRepository = uploadedFilesRepository;
  }

  @PostMapping("/upload")
  public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
      @RequestParam(value = "newFileName", required = false) String newFileName) {

    double maxFileSize = 1.3333 * 1024 * 1024; // 1,3333 Mo => j'ai mis ça pour tester la troncature à 1.33 dans le message d'erreur
    double maxFileSizeInMo = BigDecimal.valueOf(maxFileSize / (1024 * 1024))
        .setScale(2, RoundingMode.HALF_UP)
        .doubleValue();

    UploadedFilesEntity uploadedFile = new UploadedFilesEntity();
    uploadedFile.setAction("upload");

    // modifier le nom du fichier si un nouveau nom était dans la requête
    String fileName = newFileName != null ? newFileName : file.getOriginalFilename();
    uploadedFile.setFilename(fileName);

    String contentType = file.getContentType();
    if (contentType == null || (!contentType.equals("application/pdf") && !contentType.equals("image/png"))) {
      uploadedFile.setSuccess(false);
      uploadedFile.setMessage("Erreur d'upload, le type d'objet est : " + contentType);
      uploadedFilesRepository.save(uploadedFile);
      return ResponseEntity.badRequest().body("Seuls les fichiers PDF et PNG sont autorisés.");
    }

    try {
      long fileSize = file.getSize();
      if (fileSize > maxFileSize) {
        // Gérer le dépassement de la taille maximale autorisée
        String msg = "La taille du fichier dépasse la limite maximale autorisée : " + maxFileSizeInMo + " MO";
        LOG.warn(msg);
        throw new IllegalArgumentException(msg);
      }

      // vérifier si le fichier existe déjà dans le bucket
      if (fileService.fileExists("minio-test", fileName)) {
        //s'il existe, le supprimer (permet d'écraser en ré-uploadant un fichier, à voir si on veut ce fonctionnement)
        fileService.removeFile("minio-test", fileName);
        UploadedFilesEntity file2 = new UploadedFilesEntity();
        file2.setFilename(fileName);
        file2.setAction("delete");
        file2.setMessage(fileName + " supprimé, avant réinsertion");
        file2.setSuccess(true);
        uploadedFilesRepository.save(file2);
      }
      fileService.uploadFile("minio-test", fileName, file.getInputStream());
      uploadedFile.setSuccess(true);
      uploadedFile.setMessage(fileName + " uploadé avec succès");
      uploadedFilesRepository.save(uploadedFile);
      return ResponseEntity.ok("File uploaded successfully!");
    } catch (Exception e) {
      LOG.error("Failed upload due to: {}", e.getMessage());
      uploadedFile.setMessage("Echec d'upload de " + file.getOriginalFilename() + " à cause de : " + e.getMessage());
      uploadedFile.setSuccess(false);
      uploadedFilesRepository.save(uploadedFile);
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

    UploadedFilesEntity file = new UploadedFilesEntity();
    file.setFilename(fileName);
    file.setAction("download");

    try {
      // vérifier si le fichier existe déjà dans le bucket
      if (fileService.fileExists("minio-test", fileName)) {
        InputStream inputStream = fileService.downloadFile("minio-test", fileName);
        ByteArrayResource resource = new ByteArrayResource(IOUtils.toByteArray(inputStream));
        file.setSuccess(true);
        file.setMessage(fileName + " téléchargé");
        uploadedFilesRepository.save(file);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .body(resource);
      } else {
        String errorMessage = "Le fichier '" + fileName + "' n'existe pas.";
        file.setMessage(errorMessage);
        file.setSuccess(false);
        uploadedFilesRepository.save(file);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
      }
    } catch (Exception e) {
      LOG.error("Failed upload due to: {}", e.getMessage());
      file.setSuccess(false);
      file.setMessage(e.getMessage());
      uploadedFilesRepository.save(file);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/delete/{fileName}")
  public ResponseEntity<String> deleteFile(@PathVariable("fileName") String fileName) {

    UploadedFilesEntity file = new UploadedFilesEntity();
    file.setFilename(fileName);
    file.setAction("delete");
    try {
      // vérifier si le fichier existe déjà dans le bucket
      if (!fileService.fileExists("minio-test", fileName)) {
        file.setSuccess(false);
        file.setMessage("Le fichier" + fileName + " n'existe pas.");
        uploadedFilesRepository.save(file);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Le fichier '" + fileName + "' n'existe pas.");
      }

      // Appel à la méthode de suppression du service
      fileService.removeFile("minio-test", fileName);
      file.setSuccess(true);
      file.setMessage("Fichier " + fileName + " supprimé avec succès");
      uploadedFilesRepository.save(file);
      return ResponseEntity.ok("Fichier supprimé avec succès.");

    } catch (Exception e) {
      file.setSuccess(false);
      file.setMessage("Erreur lors de la suppression du fichier: " + fileName + " : " + e.getMessage());
      uploadedFilesRepository.save(file);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Erreur lors de la suppression du fichier: " + e.getMessage());
    }
  }

  @GetMapping("/verify/{filename}")
  public ResponseEntity<String> findFile(@PathVariable("filename") String fileName) {
    UploadedFilesEntity file = new UploadedFilesEntity();
    file.setFilename(fileName);
    file.setAction("checkPresence");
    try {
      // vérifier si le fichier existe déjà dans le bucket
      if (!fileService.fileExists("minio-test", fileName)) {
        file.setSuccess(false);
        file.setMessage("Le fichier" + fileName + " n'existe pas.");
        uploadedFilesRepository.save(file);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Le fichier '" + fileName + "' n'existe pas.");
      }
      file.setSuccess(true);
      file.setMessage("Accès au nom du fichier : " + fileName);
      uploadedFilesRepository.save(file);
      return ResponseEntity.ok("Le fichier '" + fileName + "' est bien stocké ici.");

    } catch (Exception e) {
      file.setSuccess(false);
      file.setMessage("Erreur lors de l'accès au fichier: " + fileName + " : " + e.getMessage());
      uploadedFilesRepository.save(file);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Erreur lors de de l'accès au fichier: " + e.getMessage());
    }
  }

  @GetMapping("listAllFiles/{bucketName}")
  public ResponseEntity<String> findAllFiles(@PathVariable("bucketName") String bucketName) throws MinioException {
    List<String> filesList = fileService.getAllFiles(bucketName);
    UploadedFilesEntity file = new UploadedFilesEntity();
    file.setFilename(bucketName);
    file.setAction("bucketFilesList");
    file.setSuccess(true);
    file.setMessage("Accès à la liste des fichiers du bucket : " + bucketName);
    uploadedFilesRepository.save(file);
    return ResponseEntity.ok("Fichiers présents dans le bucket : " + filesList.toString());
  }
}
