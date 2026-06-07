package chem_data_platform.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Create project request DTO
 * Supports public (PUBLIC) and private (PRIVATE) projects
 */
public class CreateProjectDTO {

    @NotBlank(message = "Project name cannot be empty")
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "Project description cannot be empty")
    @JsonProperty("description")
    private String description;

    @NotBlank(message = "Visibility cannot be empty")
    @Pattern(regexp = "^(PUBLIC|PRIVATE)$", message = "Visibility must be PUBLIC or PRIVATE")
    @JsonProperty("visibility")
    private String visibility; // PUBLIC or PRIVATE

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
