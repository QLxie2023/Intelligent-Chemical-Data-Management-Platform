package chem_data_platform.demo.dto;

/**
 * 文件上传响应 DTO
 */
public class FileUploadResponseDTO {

    private Long fileId;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private String uploadTimestamp;
    private Long analysisId;
    private String analysisSummary;

    public FileUploadResponseDTO() {
    }

    public FileUploadResponseDTO(Long fileId, String fileName, String fileType, String fileUrl, String uploadTimestamp) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileUrl = fileUrl;
        this.uploadTimestamp = uploadTimestamp;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public String getAnalysisSummary() {
        return analysisSummary;
    }

    public void setAnalysisSummary(String analysisSummary) {
        this.analysisSummary = analysisSummary;
    }

    // Getters and Setters
    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(String uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }
}
