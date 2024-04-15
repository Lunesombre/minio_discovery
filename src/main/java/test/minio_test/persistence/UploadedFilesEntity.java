package test.minio_test.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "uploaded_files", schema = "minio")
public class UploadedFilesEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String filename;

  @Column(nullable = false)
  private String action;

  @Column(nullable = false)
  private Timestamp occurredAt;

  @Column(nullable = false)
  private boolean success;

  @Column(nullable = false)
  private String message;


  /**
   * Méthode de rappel exécutée avant la persistance de l'entité.
   * <br>
   * Définit le timestamp 'occurredAt' à l'heure système actuelle.
   */
  @PrePersist
  public void onCreate() {
    this.occurredAt = new Timestamp(System.currentTimeMillis());
  }

}
