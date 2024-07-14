package com.example.lm.Dao;

import com.example.lm.Model.FileInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileInfoDao extends JpaRepository<FileInfo, Integer> {
    FileInfo save(FileInfo fi);

    @Query("SELECT f FROM FileInfo f WHERE f.resourcesId = :folderId AND f.isbn LIKE %:isbn%")
    List<FileInfo> findByResourcesIdAndIsbnContaining(@Param("folderId") int folderId, @Param("isbn") String isbn);

    List<FileInfo> findByResourcesId(int resourcesId);

    FileInfo findByisbn(String ISBN);

    Page<FileInfo> findByTitleContaining(String keyword, Pageable pageable);

 }
