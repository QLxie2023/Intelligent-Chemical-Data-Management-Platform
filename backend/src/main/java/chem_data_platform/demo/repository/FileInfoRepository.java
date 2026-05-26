package chem_data_platform.demo.repository;

import chem_data_platform.demo.entity.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, Long> {
    long countByUploaderId(Long uploaderId);
    List<FileInfo> findByUploaderIdOrderByUploadTimestampDesc(Long uploaderId);
}