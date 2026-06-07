package chem_data_platform.demo.repository;

import chem_data_platform.demo.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    // Additional query methods can be added as needed
}
