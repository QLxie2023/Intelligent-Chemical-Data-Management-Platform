package chem_data_platform.demo.repository;

import chem_data_platform.demo.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    // 额外查询方法可按需添加
}
