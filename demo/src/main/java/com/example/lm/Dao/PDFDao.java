package com.example.lm.Dao;

import com.example.lm.Model.PDFs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PDFDao extends JpaRepository<PDFs, Integer> {
    List<PDFs> findByResourcesId(int resourcesID);
    boolean existsByName(String name);
    List<PDFs> findByIdIn(List<Integer> ids);
    void deleteByName(String name);
    @Query("SELECT p FROM PDFs p WHERE p.name = :name ORDER BY p.id DESC")
    Optional<PDFs> findLastByName(@Param("name") String name);

}
