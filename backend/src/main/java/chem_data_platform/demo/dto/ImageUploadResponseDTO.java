package chem_data_platform.demo.dto;

/**
 * 图片上传响应 DTO
 */
public class ImageUploadResponseDTO {

    private Long imageId;
    private String imageName;
    private String imageUrl;
    private String uploadTimestamp;

    // 新增：分析结果关联
    private Long analysisId;
    private String analysisSummary;

    // 构造器
    public ImageUploadResponseDTO() {
    }

    public ImageUploadResponseDTO(Long imageId, String imageName, String imageUrl, String uploadTimestamp) {
        this.imageId = imageId;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.uploadTimestamp = uploadTimestamp;
    }

    // 可选构造器，包含分析字段
    public ImageUploadResponseDTO(Long imageId, String imageName, String imageUrl, String uploadTimestamp, Long analysisId, String analysisSummary) {
        this.imageId = imageId;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.uploadTimestamp = uploadTimestamp;
        this.analysisId = analysisId;
        this.analysisSummary = analysisSummary;
    }

    // Getters and Setters
    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(String uploadTimestamp) {
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
}
