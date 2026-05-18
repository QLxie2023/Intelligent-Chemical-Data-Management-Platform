package chem_data_platform.demo.dto;

import java.util.List;

public class SearchResultDTO {

    private Long projectId;
    private String projectName;
    private String ownerUsername;
    private String visibility;
    private Long fileId;
    private Long imageId;
    private String fileName;
    private String fileType;
    private List<String> keywords;
    private String summary;
    private String standardizedName;

    public SearchResultDTO() {
    }

    // Getters and Setters
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public Long getImageId() { return imageId; }
    public void setImageId(Long imageId) { this.imageId = imageId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getStandardizedName() { return standardizedName; }
    public void setStandardizedName(String standardizedName) { this.standardizedName = standardizedName; }
}
