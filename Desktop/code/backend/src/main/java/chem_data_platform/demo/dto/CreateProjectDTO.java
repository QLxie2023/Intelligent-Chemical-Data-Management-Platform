package chem_data_platform.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 创建项目请求 DTO
 * 支持公开（PUBLIC）和私有（PRIVATE）项目
 */
public class CreateProjectDTO {

    @NotBlank(message = "项目名称不能为空")
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "项目描述不能为空")
    @JsonProperty("description")
    private String description;

    @NotBlank(message = "可见性不能为空")
    @Pattern(regexp = "^(PUBLIC|PRIVATE)$", message = "可见性只能是 PUBLIC 或 PRIVATE")
    @JsonProperty("visibility")
    private String visibility; // PUBLIC（公开）或 PRIVATE（私有）

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
}
