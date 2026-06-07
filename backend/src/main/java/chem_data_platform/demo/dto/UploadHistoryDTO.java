package chem_data_platform.demo.dto;

import java.time.LocalDateTime;

/**
 * Upload history record DTO
 */
public class UploadHistoryDTO {
    private Long id;
    private String type; // "file" or "image"
    private String fileName;
    private String uploadTimestamp;

    public UploadHistoryDTO() {
    }

    public UploadHistoryDTO(Long id, String type, String fileName, String uploadTimestamp) {
        this.id = id;
        this.type = type;
        this.fileName = fileName;
        this.uploadTimestamp = uploadTimestamp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(String uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }
}