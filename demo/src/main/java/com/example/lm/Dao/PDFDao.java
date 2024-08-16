package com.example.lm.Dao;

import com.example.lm.Model.PDFs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PDFDao extends JpaRepository<PDFs, Integer> {
    List<PDFs> findByResourcesId(int resourcesID);
    boolean existsByName(String name);
    List<PDFs> findByIdIn(List<Integer> ids);

}
