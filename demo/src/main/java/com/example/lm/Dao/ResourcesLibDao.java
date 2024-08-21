package com.example.lm.Dao;

import com.example.lm.Model.ResourcesLib;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ResourcesLibDao extends JpaRepository<ResourcesLib, Integer> {
   ResourcesLib save(ResourcesLib rl);

   ResourcesLib findResourcesLibById(Integer id);

   List<ResourcesLib> findByIdIn(Set<Integer> ids);

   @Modifying
   @Transactional
   void deleteResourcesLibById(int folderID);

   List<ResourcesLib> findByNameContainingIgnoreCaseOrAlternateNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
           String name, String alternateName, String description);

   @Query("SELECT r FROM ResourcesLib r WHERE " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.alternateName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "r.type = :type")
   Page<ResourcesLib> searchResources(String searchTerm, String type, Pageable pageable);

   @Query("SELECT r FROM ResourcesLib r WHERE " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.alternateName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "r.display = :status")
   Page<ResourcesLib> searchResourcesByStatus(String searchTerm, String status, Pageable pageable);

   @Query("SELECT r FROM ResourcesLib r WHERE " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.alternateName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "r.display = :status AND " +
           "r.type = :type")
   Page<ResourcesLib> searchResourcesByStatusAndType(String searchTerm, String status, String type, Pageable pageable);

   Page<ResourcesLib> findByNameContainingIgnoreCaseOrAlternateNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
           String name, String alternateName, String description, Pageable pageable);


   @Query("SELECT e.name FROM ResourcesLib e")
   List<String> getAll();
}
