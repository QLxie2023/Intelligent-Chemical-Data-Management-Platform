package chem_data_platform.demo.repository;

import chem_data_platform.demo.entity.ChemicalInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChemicalInfoRepository extends JpaRepository<ChemicalInfo, Long> {
}
