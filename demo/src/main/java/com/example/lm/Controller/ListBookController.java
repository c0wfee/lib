package com.example.lm.Controller;

import com.example.lm.Dao.PDFDao;
import com.example.lm.Model.FileInfo;
import com.example.lm.Model.ResourcesLib;
import com.example.lm.Service.FileService;
import com.example.lm.Service.ResourcesLibService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ListBookController {

    @Autowired
    private FileService fileService;

    @Autowired
    private ResourcesLibService resourcesLibService;

    @Autowired
    private PDFDao pdfRepository;

    @Value("${PDFUploadPath}")
    private String PDFUploadPath;

    @GetMapping("test/search2")
    public String searchBooks(@RequestParam(required = false) String searchValue,
                              @RequestParam(required = false) String searchType,
                              @RequestParam(required = false) String publisher,
                              @RequestParam(required = false) String sourceType,
                              @RequestParam(required = false) String language,
                              @RequestParam(required = false) String published,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) Integer databaseId,
                              Model model) {

        String title = null;
        String isbn = null;
        String alternativeTitle = null;
        String author = null;
        List<FileInfo> fileInfos = new ArrayList<>();
        if ("allkinds".equalsIgnoreCase(searchType) && searchValue != null && !searchValue.equals("")) {
            fileInfos = fileService.findByAllKinds(searchValue, searchValue, searchValue, searchValue, status, publisher, sourceType, language, published, databaseId);
        }  else if ("id".equals(searchType)) {
            FileInfo  file= fileService.getFileById(Integer.parseInt(searchValue));
            fileInfos.add(file);
        }
        else {
            if ("title".equals(searchType)) {
                title = searchValue;
            } else if ("isbn".equals(searchType)) {
                isbn = searchValue;
            } else if ("alternativeTitle".equals(searchType)) {
                alternativeTitle = searchValue;
            } else if ("author".equals(searchType)) {
                author = searchValue;
            }
            fileInfos = fileService.searchBooks(title, isbn, alternativeTitle, author, status, publisher, sourceType, language, published, databaseId);
        }

        // Collect all resource IDs
        Set<Integer> resourceIds = fileInfos.stream()
                .map(FileInfo::getResourcesId)
                .collect(Collectors.toSet());

        // Fetch all ResourcesLib objects in one batch
        Map<Integer, ResourcesLib> resourcesLibMap = resourcesLibService.findResourcesLibByIds(resourceIds);

        List<Map<String, Object>> resultSet = fileInfos.stream()
                .map(fileInfo -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("title", fileInfo.getTitle());
                    result.put("alternativeTitle", fileInfo.getAlternativeTitle());
                    result.put("sourceType", fileInfo.getSourceType());
                    result.put("author", fileInfo.getAuthors());
                    result.put("isbn", fileInfo.getIsbn());
                    result.put("publisher", fileInfo.getPublisher());
                    result.put("published", fileInfo.getPublished());
                    result.put("status", fileInfo.getStatus());
                    result.put("view", fileInfo.getView());
                    result.put("download", fileInfo.getDownload());
                    result.put("downloadLink", fileInfo.getDownloadLink());
                    result.put("epubPath", fileInfo.getEpubPath());

                    // Get ResourcesLib object from the map
                    ResourcesLib resourcesLib = resourcesLibMap.get(fileInfo.getResourcesId());
                    String resourcesLibName = (resourcesLib != null) ? resourcesLib.getName() : "";
                    result.put("resourcesId", resourcesLibName);//这个其实是名称
                    result.put("resources", fileInfo.getResourcesId());//这个才是id

                    result.put("loanLabel", fileInfo.getLoanLabel());
                    int id = fileInfo.getId();
                    result.put("loaned", fileInfo.getBorrowPeriod());
                    result.put("id", id);
                    return result;
                }).toList();  // Collect into a Set to remove duplicates

        List<Map<String, Object>> resultList = new ArrayList<>(resultSet);
        int resultCount = resultList.size();
        model.addAttribute("books", new ArrayList<>(resultSet));
        model.addAttribute("resultCount", resultCount);

        // Pass search parameters to the view
        model.addAttribute("title", title);
        model.addAttribute("publisher", publisher);
        model.addAttribute("sourceType", sourceType);
        model.addAttribute("language", language);
        model.addAttribute("published", published);
        model.addAttribute("status", status);
        model.addAttribute("databaseId", databaseId);

        // Fetch distinct values once
        List<String> publishers = fileService.getAllDistinctPublishers();
        List<String> publisheds = fileService.getAllDistinctPublished();
        List<String> sourceTypes = fileService.getAllDistinctSourceType();
        List<String> languages = fileService.getAllDistinctLanguage();
        List<String> statuses = fileService.getAllDistinctStatus();
        List<Integer> databaseIds = fileService.getAllDistinctDatabaseId();
        Map<Integer, String> databaseInfoMap = databaseIds.stream()
                .collect(Collectors.toMap(id -> id, id -> {
                    ResourcesLib resourcesLib = resourcesLibService.findResourcesLibById(id);
                    return (resourcesLib != null) ? resourcesLib.getName() : "Unknown Database";
                }));

        model.addAttribute("databaseInfoMap", databaseInfoMap);
        model.addAttribute("publishers", publishers);
        model.addAttribute("publisheds", publisheds);
        model.addAttribute("sourceTypes", sourceTypes);
        model.addAttribute("languages", languages);
        model.addAttribute("statuses", statuses);
        model.addAttribute("databaseMap", databaseInfoMap);

        return "admin/searchResults";  // Return view name
    }

    @PostMapping("test/deleteList/{fileID}")
    public String deleteFile(@PathVariable("fileID") int fileID,
                             @RequestParam(required = false) String title,
                             @RequestParam(required = false) String publisher,
                             @RequestParam(required = false) String sourceType,
                             @RequestParam(required = false) String language,
                             @RequestParam(required = false) String published,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) Integer databaseId) {
        fileService.deleteBook(fileID);
        String redirectUrl = String.format("/test/search2?title=%s&publisher=%s&sourceType=%s&language=%s&published=%s&status=%s&databaseId=%s",
                encodeParam(title), encodeParam(publisher), encodeParam(sourceType), encodeParam(language), encodeParam(published), encodeParam(status), encodeParam(databaseId));

        return "redirect:" + redirectUrl;

    }

    @GetMapping("test/book/{id}")
    public String getBookDetails(@PathVariable Integer id, Model model,
                                 @RequestParam(required = false) String title,
                                 @RequestParam(required = false) String publisher,
                                 @RequestParam(required = false) String sourceType,
                                 @RequestParam(required = false) String language,
                                 @RequestParam(required = false) String published,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) Integer databaseId) {
        FileInfo fileInfo = fileService.getBookById(id);
        if (fileInfo != null) {
            model.addAttribute("title", title);
            model.addAttribute("publisher", publisher);
            model.addAttribute("sourceType", sourceType);
            model.addAttribute("language", language);
            model.addAttribute("published", published);
            model.addAttribute("status", status);
            model.addAttribute("databaseId", databaseId);
            model.addAttribute("book", fileInfo);

            return "admin/viewDetails";  // 返回书籍详细信息的视图名称
        } else {
            return "redirect:/test/searchresults";  // 如果找不到书籍，重定向回搜索结果页面
        }
    }

    @PostMapping("/book/update")
    public String updateBook(@ModelAttribute FileInfo fileInfo,
                             @RequestParam String titleSearched,
                             @RequestParam String publisherSearched,
                             @RequestParam String sourceTypeSearched,
                             @RequestParam String languageSearched,
                             @RequestParam String publishedSearched,
                             @RequestParam String statusSearched,
                             @RequestParam String databaseIdSearched) {
        fileService.updateBook(fileInfo);

        return String.format("redirect:/test/search2?title=%s&publisher=%s&sourceType=%s&language=%s&published=%s&status=%s&databaseId=%s",
                titleSearched, publisherSearched, sourceTypeSearched, languageSearched, publishedSearched, statusSearched, databaseIdSearched);
    }

    @PostMapping("/uploadSinglePDF")
    @ResponseBody
    public ResponseEntity<String> uploadPDF(@RequestParam("file") MultipartFile file,
                                            @RequestParam("id") Integer id) {
        System.out.println(id);
        try {
            fileService.uploadSingleFile(file, id);
            return ResponseEntity.ok("File uploaded successfully.");
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/uploadSingleEpub")
    @ResponseBody
    public ResponseEntity<String> uploadEpub(@RequestParam("file") MultipartFile file,
                                            @RequestParam("id") Integer id) {
        System.out.println(id);
        try {
            fileService.uploadSingleEpub(file, id);
            return ResponseEntity.ok("File uploaded successfully.");
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File upload failed: " + e.getMessage());
        }
    }

    @PostMapping("test/updateField")
    @ResponseBody
    public ResponseEntity<String> updateField(@RequestParam("id") int id,
                                              @RequestParam("editType") String editType,
                                              @RequestParam("editField") String editField) {
        try {
            fileService.updateField(id, editType, editField);
            return ResponseEntity.ok("Field updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating field");
        }
    }

    @GetMapping("/isBorrowed/{bookId}")
    public ResponseEntity<Boolean> isBookBorrowed(@PathVariable Integer bookId) {
        boolean isBorrowed = fileService.isBookBorrowed(bookId);
        return ResponseEntity.ok(isBorrowed);
    }

    private String encodeParam(Object param) {
        return param == null ? "" : URLEncoder.encode(param.toString(), StandardCharsets.UTF_8);
    }

    @GetMapping("/listPDFs")
    public String getDistinctDownloadLinkBooks(@RequestParam(required = false) Integer databaseId,
                                               @RequestParam(required = false) String searchValue,
                                               @RequestParam(required = false) String searchType,
                                               Model model) {
        List<FileInfo> pdfList;
        if (databaseId == null) {
            pdfList = fileService.getListPDFs(null); // 传递 null 给 service 方法处理所有数据库
        } else {
            pdfList = fileService.getListPDFs(databaseId);
        }

        // 获取所有 PDF 的名称，通过 downloadLink 关联
        List<String> downloadLinks = pdfList.stream()
                .map(FileInfo::getDownloadLink)
                .filter(link -> link != null && link.matches("\\d+"))  // 过滤出仅包含数字的字符串
                .distinct()
                .collect(Collectors.toList());


        Map<String, String> pdfNames = fileService.getPdfNamesByLinks(downloadLinks);

        System.out.println(pdfNames);

        // 根据 searchType 和 searchValue 进行过滤
        List<FileInfo> filteredPdfList;
        if ("all".equalsIgnoreCase(searchType)) {
            filteredPdfList = pdfList.stream()
                    .filter(fileInfo ->
                            (searchValue == null ||
                                    pdfNames.getOrDefault(fileInfo.getDownloadLink(), "").contains(searchValue) ||
                                    fileInfo.getTitle().contains(searchValue) ||
                                    fileInfo.getIsbn().contains(searchValue)))
                    .collect(Collectors.toList());
        } else if ("name".equalsIgnoreCase(searchType)) {
            filteredPdfList = pdfList.stream()
                    .filter(fileInfo -> searchValue == null ||
                            pdfNames.getOrDefault(fileInfo.getDownloadLink(), "").contains(searchValue))
                    .collect(Collectors.toList());
        } else if ("title".equalsIgnoreCase(searchType)) {
            filteredPdfList = pdfList.stream()
                    .filter(fileInfo -> searchValue == null || fileInfo.getTitle().contains(searchValue))
                    .collect(Collectors.toList());
        } else if ("isbn".equalsIgnoreCase(searchType)) {
            filteredPdfList = pdfList.stream()
                    .filter(fileInfo -> searchValue == null || fileInfo.getIsbn().contains(searchValue))
                    .collect(Collectors.toList());
        } else {
            filteredPdfList = pdfList;
        }

        // 按照 downloadLink 分组
        Map<String, List<FileInfo>> groupedByDownloadLink = filteredPdfList.stream()
                .filter(fileInfo -> {
                    String downloadLink = fileInfo.getDownloadLink();
                    return downloadLink != null && downloadLink.matches("\\d+");  // 过滤非数字的 downloadLink
                })
                .collect(Collectors.groupingBy(FileInfo::getDownloadLink));

        // 获取所有数据库的 ID 和名称的映射
        Map<Integer, String> allDatabase = resourcesLibService.getAllDatabaseIdsAndNames();

        // 为每个 FileInfo 查找对应的数据库名称，并将其添加到模型中
        Map<String, String> fileInfoDatabaseNames = new HashMap<>();
        for (FileInfo fileInfo : pdfList) {
            String dbName = allDatabase.get(fileInfo.getResourcesId());
            fileInfoDatabaseNames.put(fileInfo.getDownloadLink(), dbName);
        }

        Set<Integer> uniqueNoTitles = groupedByDownloadLink.values().stream()
                .map(List::size)
                .collect(Collectors.toSet());

        System.out.println(groupedByDownloadLink);

        model.addAttribute("pdfs", groupedByDownloadLink);
        model.addAttribute("pdfNames", pdfNames);
        model.addAttribute("database", databaseId); // 始终添加 databaseId，即使是 null
        model.addAttribute("uniqueNoTitles", uniqueNoTitles);
        model.addAttribute("databaseNames", fileInfoDatabaseNames);
        model.addAttribute("allDatabase", allDatabase);

        return "admin/pdfList";
    }

    @PostMapping("/deletePdf/{pdfID}")
    public String deletePdf(@PathVariable String pdfID, @RequestParam(required = false) Integer databaseId, Model model) {
        boolean isDeleted = fileService.deletePdfById(pdfID,databaseId);
        if (isDeleted) {
            model.addAttribute("message", "PDF deleted successfully.");
        } else {
            model.addAttribute("message", "Failed to delete PDF.");
        }
        if (databaseId==null){
            return "redirect:/listPDFs";
        }

        return String.format("redirect:/listPDFs?databaseId=%s",databaseId);
    }

    @PostMapping("deleteList2/{fileID}")
    public String deleteFile2(@PathVariable("fileID") Integer fileID,
                             @RequestParam Integer databaseId
                             ) {
        fileService.deleteBook(fileID);
        if (databaseId==null){
            return "redirect:/listPDFs";
        }
        return String.format("redirect:/listPDFs?databaseId=%s",databaseId);
    }

    @GetMapping("/listEpubs")
    public String getDistinctEpubs(@RequestParam(required = false) Integer databaseId,
                                               @RequestParam(required = false) String searchValue,
                                               @RequestParam(required = false) String searchType,
                                               Model model) {
        // 获取所有 PDF 文件
        List<FileInfo> pdfList = fileService.getListEpubs(databaseId);
        System.out.println(pdfList);

        // 获取所有 PDF 的名称，通过 downloadLink 关联
        List<String> path = pdfList.stream()
                .map(FileInfo::getEpubPath)
                .filter(epubPath -> epubPath != null && !"NULL".equals(epubPath))  // 过滤掉 "NULL" 字符串
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> epubNames = fileService.getPdfNamesByLinks(path);

        // 根据 searchType 和 searchValue 进行过滤
        List<FileInfo> filteredPdfList;
        if ("all".equalsIgnoreCase(searchType)) {
            // 如果 searchType 为 "all"，在 name、title 和 isbn 中搜索 searchValue
            filteredPdfList = pdfList.stream()
                    .filter(fileInfo ->
                            (searchValue == null ||
                                    epubNames.getOrDefault(fileInfo.getEpubPath(), "").contains(searchValue) ||
                                    fileInfo.getTitle().contains(searchValue) ||
                                    fileInfo.getIsbn().contains(searchValue)))
                    .collect(Collectors.toList());
        } else if ("name".equalsIgnoreCase(searchType)) {
            filteredPdfList = pdfList.stream()
                    .filter(fileInfo -> searchValue == null ||
                            epubNames.getOrDefault(fileInfo.getEpubPath(), "").contains(searchValue))
                    .collect(Collectors.toList());
        } else if ("title".equalsIgnoreCase(searchType)) {
            filteredPdfList = pdfList.stream()
                    .filter(fileInfo -> searchValue == null || fileInfo.getTitle().contains(searchValue))
                    .collect(Collectors.toList());
        } else if ("isbn".equalsIgnoreCase(searchType)) {
            filteredPdfList = pdfList.stream()
                    .filter(fileInfo -> searchValue == null || fileInfo.getIsbn().contains(searchValue))
                    .collect(Collectors.toList());
        } else {
            filteredPdfList = pdfList;
        }

        // 按照 downloadLink 分组
        Map<String, List<FileInfo>> groupedByepubPath = filteredPdfList.stream()
                .collect(Collectors.groupingBy(FileInfo::getEpubPath));

        Set<Integer> uniqueNoTitles = groupedByepubPath.values().stream()
                .map(List::size)
                .collect(Collectors.toSet());
        Map<Integer, String> allDatabase = resourcesLibService.getAllDatabaseIdsAndNames();

        // 为每个 FileInfo 查找对应的数据库名称，并将其添加到模型中
        Map<String, String> fileInfoDatabaseNames = new HashMap<>();
        for (FileInfo fileInfo : pdfList) {
            String dbName = allDatabase.get(fileInfo.getResourcesId());
            fileInfoDatabaseNames.put(fileInfo.getDownloadLink(), dbName);
        }
        model.addAttribute("pdfs", groupedByepubPath);
        model.addAttribute("pdfNames", epubNames);
        model.addAttribute("database", databaseId); // 始终添加 databaseId，即使是 null
        model.addAttribute("uniqueNoTitles", uniqueNoTitles);
        model.addAttribute("databaseNames", fileInfoDatabaseNames);
        model.addAttribute("allDatabase", allDatabase);

        return "admin/epubList";
    }

    @PostMapping("/deleteEpub/{pdfID}")
    public String deleteEpub(@PathVariable String pdfID, @RequestParam(required = false) Integer databaseId, Model model) {
        boolean isDeleted = fileService.deleteEpubById(pdfID,databaseId);
        if (isDeleted) {
            model.addAttribute("message", "PDF deleted successfully.");
        } else {
            model.addAttribute("message", "Failed to delete PDF.");
        }
        if (databaseId==null){
            return "redirect:/listEpubs";
        }
        return String.format("redirect:/listEpubs?databaseId=%s",databaseId);
    }

    @PostMapping("deleteList3/{fileID}")
    public String deleteFile3(@PathVariable("fileID") int fileID,
                              @RequestParam Integer databaseId
    ) {
        if (databaseId==null){
            return "redirect:/listEpubs";
        }
        fileService.deleteBook(fileID);
        return String.format("redirect:/listEpubs?databaseId=%s",databaseId);
    }

    @GetMapping("/files-view")
    public String fileList() {
        return "user/fileList";  // 这里返回的是视图名称，不包括.html扩展名
    }


}
