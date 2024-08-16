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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        if ("allkinds".equalsIgnoreCase(searchType) && searchValue != null) {
            fileInfos = fileService.findByAllKinds(searchValue, searchValue, searchValue, searchValue, status, publisher, sourceType, language, published, databaseId);
//            System.out.println(fileInfos);
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

                    // Get ResourcesLib object from the map
                    ResourcesLib resourcesLib = resourcesLibMap.get(fileInfo.getResourcesId());
                    String resourcesLibName = (resourcesLib != null) ? resourcesLib.getName() : "";
                    result.put("resourcesId", resourcesLibName);

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

            return "viewDetails";  // 返回书籍详细信息的视图名称
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

        String redirectUrl = String.format("redirect:/test/search2?title=%s&publisher=%s&sourceType=%s&language=%s&published=%s&status=%s&databaseId=%s",
                titleSearched, publisherSearched, sourceTypeSearched, languageSearched, publishedSearched, statusSearched, databaseIdSearched);
        return redirectUrl;
    }

//    @PutMapping("/updateStatus/{id}")
//    public ResponseEntity<String> updateStatus(@PathVariable int id, @RequestParam String newStatus) {
//        fileService.updateStatus(id, newStatus);
//        return ResponseEntity.ok("Status updated successfully");
//    }
//
//    @PutMapping("/updateLoan/{id}")
//    public ResponseEntity<String> updateLoan(@PathVariable int id, @RequestParam String newLoan) {
//        fileService.updateLoan(id, newLoan);
//        return ResponseEntity.ok("Loan updated successfully");
//    }

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
        try {
            return param == null ? "" : URLEncoder.encode(param.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

//    @GetMapping("/downloadFromList/{bookId}")
//    public ResponseEntity<Resource> downloadFile2(@PathVariable int bookId) {
//        FileInfo f = fileService.getBookById(bookId);
//
//        // Use regex to split by spaces and parentheses, removing non-digit parts
//        String[] parts = f.getIsbn().replaceAll("[^\\d\\s]", "").split("\\s+");
//
//        List<String> resultList = new ArrayList<>(Arrays.asList(parts));
//        for (String part : parts) {
//            resultList.add(part + ".pdf");
//        }
//
//        for (String fileName : resultList) {
//            if (pdfRepository.existsByName(fileName)) {
//                System.out.println("Downloading file with name: " + fileName);
//                try {
//                    Path filePath = Paths.get(PDFUploadPath).resolve(fileName).normalize();
//                    Resource resource = new UrlResource(filePath.toUri());
//
//                    if (resource.exists()) {
//                        return ResponseEntity.ok()
//                                .contentType(MediaType.APPLICATION_PDF)
//                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
//                                .body(resource);
//                    } else {
//                        return ResponseEntity.notFound().build();
//                    }
//                } catch (MalformedURLException ex) {
//                    return ResponseEntity.badRequest().build();
//                }
//            }
//        }
//
//        return ResponseEntity.notFound().build();
//    }

    @GetMapping("/listPDFs")
    public String getDistinctDownloadLinkBooks(@RequestParam Integer databaseId,
                                               @RequestParam(required = false) String searchValue,
                                               @RequestParam(required = false) String searchType,
                                               Model model) {
        // 获取所有 PDF 文件
        List<FileInfo> pdfList = fileService.getListPDFs(databaseId);

        // 获取所有 PDF 的名称，通过 downloadLink 关联
        List<String> downloadLinks = pdfList.stream()
                .map(FileInfo::getDownloadLink)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> pdfNames = fileService.getPdfNamesByLinks(downloadLinks);

        // 根据 searchType 和 searchValue 进行过滤
        List<FileInfo> filteredPdfList;
        if ("all".equalsIgnoreCase(searchType)) {
            // 如果 searchType 为 "all"，在 name、title 和 isbn 中搜索 searchValue
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
                .collect(Collectors.groupingBy(FileInfo::getDownloadLink));

        Set<Integer> uniqueNoTitles = groupedByDownloadLink.values().stream()
                .map(List::size)
                .collect(Collectors.toSet());
        Map<Integer, String> allDatabase = resourcesLibService.getAllDatabaseIdsAndNames();

        model.addAttribute("pdfs", groupedByDownloadLink);
        model.addAttribute("pdfNames", pdfNames);
        model.addAttribute("database", databaseId);
        model.addAttribute("uniqueNoTitles", uniqueNoTitles);
        model.addAttribute("databaseName", resourcesLibService.findResourcesLibById(databaseId).getName());
        model.addAttribute("allDatabase", allDatabase);

        return "admin/pdfList";
    }

    @PostMapping("/deletePdf/{pdfID}")
    public String deletePdf(@PathVariable String pdfID, @RequestParam Integer databaseId, Model model) {
        boolean isDeleted = fileService.deletePdfById(pdfID,databaseId);
        if (isDeleted) {
            model.addAttribute("message", "PDF deleted successfully.");
        } else {
            model.addAttribute("message", "Failed to delete PDF.");
        }
        // Optionally, add logic to refresh the list of PDFs or redirect to another page

        return String.format("redirect:/listPDFs?databaseId=%s",databaseId);
    }

    @PostMapping("deleteList2/{fileID}")
    public String deleteFile2(@PathVariable("fileID") int fileID,
                             @RequestParam Integer databaseId
                             ) {
        fileService.deleteBook(fileID);
        return String.format("redirect:/listPDFs?databaseId=%s",databaseId);
    }

//    @PostMapping("/update")
//    public String updateBook2(@ModelAttribute FileInfo fileInfo,
//                              @RequestParam(required = false) String searchValue,
//                              @RequestParam(required = false) String searchType) {
//        fileService.updateBook(fileInfo);
//
//        Integer databaseId = fileInfo.getResourcesId();
//        return String.format("redirect:/listPDFs?databaseId=%s&sourceType=%s&sourceValue=%s",databaseId,searchType,searchValue);
//    }

    @GetMapping("/listEpubs")
    public String getDistinctEpubs(@RequestParam Integer databaseId,
                                               @RequestParam(required = false) String searchValue,
                                               @RequestParam(required = false) String searchType,
                                               Model model) {
        // 获取所有 PDF 文件
        List<FileInfo> pdfList = fileService.getListEpubs(databaseId);

        // 获取所有 PDF 的名称，通过 downloadLink 关联
        List<String> path = pdfList.stream()
                .map(FileInfo::getEpubPath)
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

        model.addAttribute("pdfs", groupedByepubPath);
        model.addAttribute("pdfNames", epubNames);
        model.addAttribute("database", databaseId);
        model.addAttribute("uniqueNoTitles", uniqueNoTitles);
        model.addAttribute("databaseName", resourcesLibService.findResourcesLibById(databaseId).getName());
        model.addAttribute("allDatabase", allDatabase);

        return "admin/epubList";
    }

    @PostMapping("/deleteEpub/{pdfID}")
    public String deleteEpub(@PathVariable String pdfID, @RequestParam Integer databaseId, Model model) {
        boolean isDeleted = fileService.deleteEpubById(pdfID,databaseId);
        if (isDeleted) {
            model.addAttribute("message", "PDF deleted successfully.");
        } else {
            model.addAttribute("message", "Failed to delete PDF.");
        }
        // Optionally, add logic to refresh the list of PDFs or redirect to another page

        return String.format("redirect:/listEpubs?databaseId=%s",databaseId);
    }

    @PostMapping("deleteList3/{fileID}")
    public String deleteFile3(@PathVariable("fileID") int fileID,
                              @RequestParam Integer databaseId
    ) {
        fileService.deleteBook(fileID);
        return String.format("redirect:/listEpubs?databaseId=%s",databaseId);
    }
    @GetMapping("/files-view")
    public String fileList() {
        return "user/fileList";  // 这里返回的是视图名称，不包括.html扩展名
    }


}
