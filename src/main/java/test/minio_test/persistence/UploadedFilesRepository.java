package test.minio_test.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadedFilesRepository extends JpaRepository<UploadedFilesEntity, Long> {

}
