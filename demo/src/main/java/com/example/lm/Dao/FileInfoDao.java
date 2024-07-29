package com.example.lm.Dao;

import com.example.lm.Model.FileInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    List<FileInfo> findByTitleContaining(String title);

    @Query("SELECT f FROM FileInfo f WHERE " +
            "(:title IS NULL OR f.title LIKE %:title%) " +
            "AND (:status IS NULL OR f.status = :status) " +
            "AND (:publisher IS NULL OR f.publisher = :publisher) " +
            "AND (:sourceType IS NULL OR f.sourceType = :sourceType) " +
            "AND (:language IS NULL OR f.language = :language) " +
            "AND (:published IS NULL OR f.published = :published) " +
            "AND (:databaseId IS NULL OR f.resourcesId = :databaseId)")
    List<FileInfo> searchBooks(@Param("title") String title,
                               @Param("status") String status,
                               @Param("publisher") String publisher,
                               @Param("sourceType") String sourceType,
                               @Param("language") String language,
                               @Param("published") String published,
                               @Param("databaseId") Integer databaseId);

    @Query("SELECT DISTINCT f.publisher FROM FileInfo f")
    List<String> findAllDistinctPublishers();

    @Query("SELECT DISTINCT f.published FROM FileInfo f")
    List<String> findAllDistinctPublished();

    @Query("SELECT DISTINCT f.sourceType FROM FileInfo f")
    List<String> findAllDistinctSourceType();

    @Query("SELECT DISTINCT f.language FROM FileInfo f")
    List<String> findAllDistinctLanguage();

    @Query("SELECT DISTINCT f.status FROM FileInfo f")
    List<String> findAllDistinctStatus();

    @Query("SELECT DISTINCT f.resourcesId FROM FileInfo f")
    List<Integer> findAllDistinctDatabaseId();

    @Modifying
    @Query(value = "UPDATE books SET status = :status WHERE id = :id", nativeQuery = true)
    int updateStatusById(@Param("id") int id, @Param("status") String status);

    @Modifying
    @Query(value = "UPDATE books SET loan = :loan WHERE id = :id", nativeQuery = true)
    int updateLoanById(@Param("id") int id, @Param("loan") String loan);


    Page<FileInfo> findByTitleContaining(String keyword, Pageable pageable);

    FileInfo findById(int id);

    void deleteById(int id);


}
