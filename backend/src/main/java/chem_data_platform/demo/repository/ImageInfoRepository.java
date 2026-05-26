package chem_data_platform.demo.repository;

import chem_data_platform.demo.entity.ImageInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageInfoRepository extends JpaRepository<ImageInfo, Long> {
    long countByUploaderId(Long uploaderId);
    List<ImageInfo> findByUploaderIdOrderByUploadTimestampDesc(Long uploaderId);
}