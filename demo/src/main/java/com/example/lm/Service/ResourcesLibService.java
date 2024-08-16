package com.example.lm.Service;

import com.example.lm.Dao.FileInfoDao;
import com.example.lm.Dao.ResourcesLibDao;
import com.example.lm.Model.ResourcesLib;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResourcesLibService {
    @Autowired
    private ResourcesLibDao resourcesLibDao;

    @Autowired
    private FileInfoDao fileInfoDao;

    /**
     *
     * @param name
     * @param type
     * @param title_en
     * @param description_en
     * @param title_cn
     * @param description_cn
     * displayCode: 0: Hidden; 1: Published
     *
     */
    /*public void createResourcesLib(String name, String type, String title_en,
                                   String description_en, String title_cn,
                                   String description_cn) {
        int hidden = 0;
        ResourcesLib rl = new ResourcesLib();
        rl.setName(name);
        rl.setType(type);
//        rl.setTitle_en(title_en);
//        rl.setTitle_cn(title_cn);
//        rl.setDescription_cn(description_cn);
        rl.setDescription_en(description_en);
        rl.setDisplay(hidden);
        resourcesLibDao.save(rl);
    }*/

    public List<ResourcesLib> getAllPackages() {
        return resourcesLibDao.findAll();
    }

    public void addFolder(String folderName) {
        ResourcesLib folder = new ResourcesLib();
        folder.setName(folderName);
        resourcesLibDao.save(folder);
    }

    public void renameFolder(int folderId, String newName) {
        ResourcesLib rl = resourcesLibDao.findResourcesLibById(folderId);
        rl.setName(newName);
        resourcesLibDao.save(rl);
    }

    public void saveNewDatabases(ResourcesLib rl) {
        resourcesLibDao.save(rl);
    }

    public ResourcesLib findResourcesLibById(Integer id){
        return resourcesLibDao.findResourcesLibById(id);
    }

    public Map<Integer, ResourcesLib> findResourcesLibByIds(Set<Integer> ids) {
        List<ResourcesLib> resourcesLibs = resourcesLibDao.findByIdIn(ids);
        return resourcesLibs.stream().collect(Collectors.toMap(ResourcesLib::getId, Function.identity()));
    }

    public Map<Integer, String> getAllDatabaseIdsAndNames() {
        List<ResourcesLib> databases = resourcesLibDao.findAll();
        Map<Integer, String> idNameMap = new HashMap<>();
        for (ResourcesLib database : databases) {
            idNameMap.put(database.getId(), database.getName());
        }
        return idNameMap;
    }

    public void updateFolderDetails(int folderId, String newName, String newAlternateNames, String newType, String newDescription) {
        ResourcesLib folder = resourcesLibDao.findById(folderId).orElseThrow(() -> new IllegalArgumentException("Invalid folder ID"));
        if (newName != null && !newName.isEmpty()) {
            folder.setName(newName);
        }
        if (newAlternateNames != null && !newAlternateNames.isEmpty()) {
            folder.setAlternateName(newAlternateNames);
        }
        if (newType != null && !newType.isEmpty()) {
            folder.setType(newType);
        }
        if (newDescription != null && !newDescription.isEmpty()) {
            folder.setDescription(newDescription);
        }

        resourcesLibDao.save(folder);
    }

    public void deleteFolder(int folderId) {
        resourcesLibDao.deleteResourcesLibById(folderId);
        fileInfoDao.deleteFileInfoByResourcesId(folderId);
    }

    public List<ResourcesLib> searchFolders(String query, String type, String status) {
        if ((type != null && !type.isEmpty()) && (status == null || status.isEmpty())) {
            return resourcesLibDao.searchResources(query, type);
        }
        if ((status != null && !status.isEmpty()) && (type == null || type.isEmpty())) {
            return resourcesLibDao.searchResourcesByStatus(query, status);
        }
        if ((status != null && !status.isEmpty()) && (type != null && !type.isEmpty())) {
            return resourcesLibDao.searchResourcesByStatusAndType(query, status, type);
        }
        return resourcesLibDao.findByNameContainingIgnoreCaseOrAlternateNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                query, query, query);
    }

    public void save(ResourcesLib rl){
        resourcesLibDao.save(rl);
    }
}
