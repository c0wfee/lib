package com.example.lm.Controller;

import com.example.lm.Model.*;
import com.example.lm.Service.BorrowService;
import com.example.lm.Service.FileService;
import com.example.lm.Service.ResourcesLibService;

import com.example.lm.utils.FilterData;
import com.example.lm.utils.SearchResult;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class MainController {
    @Autowired
    private FileService fileService;

    @Autowired
    private GridFSBucket gridFSBucket;

    @Autowired
    private ResourcesLibService resourcesLibService;

    private final Path fileStorageLocation = Paths.get("demo/src/main/resources/static/PDFs").normalize();

    private static final String PDF_DIRECTORY = "demo/src/main/resources/static/PDFs/";

    private static final String MARC_DIRECTORY = "demo/src/main/resources/static/Epub/";

    @Autowired
    private BorrowService borrowService;


    @GetMapping("test")
    public String upload() {
        return "upload";
    }

    @GetMapping("/table")
    public ModelAndView showTable(@RequestParam String folderId) {
        ModelAndView modelAndView = new ModelAndView("table");
        modelAndView.addObject("folderId", folderId);
        return modelAndView;
    }

    @GetMapping("/files")
    public String listFiles(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<GridFSFile> files = fileService.listFiles(keyword);
        model.addAttribute("files", files);
        return "lists";
    }

    @GetMapping("/resourcesLib")
    public String getFileExplorer(@RequestParam(name = "query", required = false, defaultValue = "") String query,
                                  @RequestParam(name = "type", required = false, defaultValue = "") String type,
                                  @RequestParam(name = "display", required = false, defaultValue = "") String status,
                                  Model model) {
        List<ResourcesLib> folders = resourcesLibService.searchFolders(query, type, status);
        Map<Integer, List<File>> folderPDFMap = new HashMap<>();
        for (ResourcesLib folder : folders) {
            List<File> PDFs = fileService.getPDFsByLib(folder.getId());
            int marcCount = fileService.getMarcNum(folder.getId());
            int pdfCount = fileService.getPDFNum(folder.getId());
            int epubCount = fileService.getEPUBNum(folder.getId());
            folder.setMarcCount(marcCount);
            folder.setPdfCount(pdfCount);
            folder.setEpubCount(epubCount);
            resourcesLibService.save(folder);
            folderPDFMap.put(folder.getId(), PDFs);
        }
        model.addAttribute("folderPDFMap", folderPDFMap);
        model.addAttribute("folders", folders);
        return "admin/sourceDatabases"; // 返回的模板名称
    }

    @PostMapping("/addDatabases")
    public String addDatabase(
            @RequestParam("databaseName") String databaseName,
            @RequestParam("alternateNames") String alternateNames,
            @RequestParam("typeSelection") String typeSelection,
            @RequestParam("databaseDescription") String databaseDescription,
            @RequestParam("status") String status,
            @RequestParam("view") String view,
            @RequestParam("download") String download,
            @RequestParam("borrow") String borrow,
            @RequestParam(value = "file", required = false) MultipartFile marcFile,
            @RequestParam(value = "excel", required = false) MultipartFile excel,
            @RequestParam(value = "epub", required = false) List<MultipartFile> epub,
            @RequestParam(value = "files", required = false) List<MultipartFile> pdfFiles,
            RedirectAttributes redirectAttributes) throws IOException {

        int borrow_period = Integer.parseInt(borrow.replaceAll("[^0-9]", ""));
        ResourcesLib resourcesLib = new ResourcesLib();
        resourcesLib.setName(databaseName);
        resourcesLib.setAlternateName(alternateNames);
        resourcesLib.setDescription(databaseDescription);
        resourcesLib.setType(typeSelection);
        resourcesLib.setDisplay(status);
        resourcesLib.setView(view);
        resourcesLib.setDownload(download);
        resourcesLib.setBorrow(borrow_period);
        resourcesLibService.saveNewDatabases(resourcesLib);

        if (marcFile != null && !marcFile.isEmpty()) {
            fileService.uploadMARCFile(resourcesLib.getId(), marcFile, status, view, download, borrow_period);
        }

        if (excel != null && !excel.isEmpty()) {
            fileService.saveExcel(resourcesLib.getId(), excel, status, view, download, borrow_period);
        }

        List<String> invalidPDF = new ArrayList<>();
        if (pdfFiles != null && !pdfFiles.isEmpty()) {
            invalidPDF = fileService.savePDFs(resourcesLib.getId(), pdfFiles);
        }

        List<String> invalidEPUB = new ArrayList<>();
        if (epub != null && !epub.isEmpty()) {
            invalidEPUB = fileService.saveEPUBs(resourcesLib.getId(), epub);
        }

        List<String> invalidFiles = new ArrayList<>();
        invalidFiles.addAll(invalidPDF);
        invalidFiles.addAll(invalidEPUB);

        if (!invalidFiles.isEmpty()) {
            redirectAttributes.addFlashAttribute("invalidFiles", invalidFiles);
            redirectAttributes.addFlashAttribute("folderIdWithInvalidFiles", resourcesLib.getId());
        }

        return "redirect:/resourcesLib";
    }



    @PostMapping("/rename-folder")
    @ResponseBody
    public String renameFolder(@RequestParam("folderId") int folderId,
                               @RequestParam(value = "newName", required = false) String newName,
                               @RequestParam(value = "newAlternateNames", required = false) String newAlternateNames,
                               @RequestParam(value = "newType", required = false) String newType,
                               @RequestParam(value = "newDescription", required = false) String newDescription) {
        try {
            resourcesLibService.updateFolderDetails(folderId, newName, newAlternateNames, newType, newDescription);
            return "redirect:/resourcesLib";
        } catch (Exception e) {
            return "Error updating folder details: " + e.getMessage();
        }
    }

    @PostMapping("/uploadMARC")
    public String uploadMarcFile(@RequestParam("folderId") int folderId, @RequestParam("file") MultipartFile marc, String status, String view, String download, String borrow) {
        int borrow_period = Integer.parseInt(borrow.replaceAll("[^0-9]", ""));
        fileService.uploadMARCFile(folderId, marc, status, view, download, borrow_period);
        return "redirect:/resourcesLib";
    }

    @PostMapping("/uploadPDF")
    public String handlePDFUpload(@RequestParam int folderId, @RequestParam("files") List<MultipartFile> files, RedirectAttributes redirectAttributes) throws IOException {
        List<String> invalidFiles = fileService.savePDFs(folderId, files);
        if (!invalidFiles.isEmpty()) {
            redirectAttributes.addFlashAttribute("invalidFiles", invalidFiles);
            redirectAttributes.addFlashAttribute("folderIdWithInvalidFiles", folderId);
        }
        return "redirect:/resourcesLib";
    }

    @PostMapping("/hideBook")
    public ResponseEntity<?> hideBooks(@RequestParam("folderId") int libId) {
        ResourcesLib rl = resourcesLibService.findResourcesLibById(libId);
        rl.setDisplay("Unpublished");
        resourcesLibService.save(rl);
        List<FileInfo> list = fileService.getMarcDetailByID(libId);
        for (FileInfo pdf : list) {
            pdf.setStatus("Unpublished");
            fileService.savePDF(pdf);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/showBook")
    public ResponseEntity<?> showBooks(@RequestParam("folderId") int libId) {
        ResourcesLib rl = resourcesLibService.findResourcesLibById(libId);
        rl.setDisplay("Published");
        resourcesLibService.save(rl);
        List<FileInfo> list = fileService.getMarcDetailByID(libId);
        for (FileInfo pdf : list) {
            pdf.setStatus("Published");
            fileService.savePDF(pdf);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/cancelView")
    public ResponseEntity<?> cancelViewBooks(@RequestParam("folderId") int libId) {
        List<FileInfo> list = fileService.getMarcDetailByID(libId);
        for (FileInfo pdf : list) {
            pdf.setView("Disable");
            fileService.savePDF(pdf);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/AbleView")
    public ResponseEntity<?> ableViewBooks(@RequestParam("folderId") int libId) {
        List<FileInfo> list = fileService.getMarcDetailByID(libId);
        for (FileInfo pdf : list) {
            pdf.setView("Enable");
            fileService.savePDF(pdf);
        }
        return ResponseEntity.ok(list);
    }

    /**
     * @param libId
     * @return Disable: 不允许下载
     * Able: 允许下载
     */
    @PostMapping("/cancelDownload")
    public ResponseEntity<?> cancelDownloadBooks(@RequestParam("folderId") int libId) {
        List<FileInfo> list = fileService.getMarcDetailByID(libId);
        for (FileInfo pdf : list) {
            pdf.setDownload("Disable");
            fileService.savePDF(pdf);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/ableDownload")
    public ResponseEntity<?> ableDownloadBooks(@RequestParam("folderId") int libId) {
        List<FileInfo> list = fileService.getMarcDetailByID(libId);
        for (FileInfo pdf : list) {
            pdf.setDownload("Enable");
            fileService.savePDF(pdf);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/cancelBorrow")
    public ResponseEntity<?> cancelBorrowBooks(@RequestParam("folderId") int libId) {
        List<FileInfo> list = fileService.getMarcDetailByID(libId);
        for (FileInfo pdf : list) {
            pdf.setBorrowPeriod(0);
            fileService.savePDF(pdf);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/ableBorrow")
    public ResponseEntity<?> ableBorrowBooks(@RequestParam("folderId") int libId, @RequestParam("borrow_period") int period) {
        List<FileInfo> list = fileService.getMarcDetailByID(libId);
        for (FileInfo pdf : list) {
            pdf.setBorrowPeriod(period);
            fileService.savePDF(pdf);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/returnBook")
    public ResponseEntity<?> returnBook(@RequestParam("bookId") int bookId) {
        FileInfo pdf = fileService.getFileById(bookId);
        pdf.setLoanLabel("Returned");
        fileService.savePDF(pdf);
        return ResponseEntity.ok(pdf);
    }

    @PostMapping("/borrowBook")
    public ResponseEntity<?> ableBorrowForBooks(@RequestParam("bookID") int bookID, @RequestParam("borrow_period") int period) {
        FileInfo pdf = fileService.getFileById(bookID);
        pdf.setLoanLabel("Borrowed");
        pdf.setBorrowPeriod(period);
        fileService.savePDF(pdf);
        Borrow borrow = new Borrow();
        borrow.setBookId(bookID);
        borrow.setBookTitle(pdf.getTitle());
        borrow.setUsername("admin");
        borrow.setLoanStartTime(String.valueOf(System.currentTimeMillis()));
        borrow.setLoanEndTime(String.valueOf(System.currentTimeMillis() + period * 24 * 60 * 60 * 1000));
        borrowService.saveBorrow(borrow, period);
        return ResponseEntity.ok(pdf);
    }

    // 处理文件下载请求
    @PostMapping("/download/{fileID}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("fileID") String fileID) throws IOException {
        GridFSFile file = fileService.getFileById(fileID);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        InputStream inputStream = gridFSBucket.openDownloadStream(file.getObjectId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMetadata().getString("_contentType")))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(new InputStreamResource(inputStream));
    }

    @PostMapping("/delete/{fileID}")
    public String deleteFile(@PathVariable("fileID") String fileID) {
        fileService.deleteFileById(fileID);
        return "redirect:/files";
    }

    @GetMapping("/marcDetails")
    public ResponseEntity<?> getMarcDetails(@RequestParam("folderId") int id) {
        List<FileInfo> marcDetails = fileService.getMarcDetailByID(id);
        return ResponseEntity.ok(marcDetails);
    }

    @PostMapping("/search")
    @ResponseBody
    public Map<String, Object> searchFiles(@RequestBody FilterData filterData) {
        // 检查参数是否为 "undefined" 字符串并将其设为 null
        if (filterData.getSeries() != null && filterData.getSeries().size() == 1 && "undefined".equals(filterData.getSeries().get(0))) {
            filterData.setSeries(null);
        }
        if (filterData.getPublisher() != null && filterData.getPublisher().size() == 1 && "undefined".equals(filterData.getPublisher().get(0))) {
            filterData.setPublisher(null);
        }
        if (filterData.getSubject() != null && filterData.getSubject().size() == 1 && "undefined".equals(filterData.getSubject().get(0))) {
            filterData.setSubject(null);
        }
        if (filterData.getDatabase() != null && filterData.getDatabase().size() == 1 && "undefined".equals(filterData.getDatabase().get(0))) {
            filterData.setDatabase(null);
        }

        Pageable pageable = PageRequest.of(filterData.getPage() - 1, filterData.getSize());
        Page<FileInfo> files = fileService.advancedSearch(
                filterData.getKeyword(),
                filterData.getSeries(),
                filterData.getPublisher(),
                filterData.getSubject(),
                filterData.getDatabase(),
                filterData.getPublishedFrom(),
                filterData.getPublishedTo(),
                filterData.getPublishedYear(),
                pageable
        );
        System.out.println("没有吗");
        for (FileInfo file : files) {
            System.out.println(file.getId());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", files.getContent());
        response.put("totalElements", files.getTotalElements());

        Map<String, Object> filters = new HashMap<>();
        filters.put("keyword", filterData.getKeyword());
        filters.put("series", filterData.getSeries());
        filters.put("publisher", filterData.getPublisher());
        filters.put("subject", filterData.getSubject());
        filters.put("database", filterData.getDatabase());
        filters.put("publishedFrom", filterData.getPublishedFrom());
        filters.put("publishedTo", filterData.getPublishedTo());
        filters.put("publishedYear", filterData.getPublishedYear());

        response.put("filters", filters);

        return response;
    }


    @GetMapping("/getBookPeriod")
    public ResponseEntity<?> getBookPeriod(@RequestParam("bookID") int bookId) {
        return ResponseEntity.ok(fileService.getBookPeriod(bookId));
    }

    @GetMapping("/book/{id}")
    public ResponseEntity<FileInfo> getBookById(@PathVariable Integer id) {
        FileInfo fileInfo = fileService.getBookById(id);
        if (fileInfo != null) {
            return ResponseEntity.ok(fileInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/saveTable")
    public ResponseEntity<String> saveTable(@RequestBody List<Map<String, String>> tableData) {
        try {
            fileService.saveMarcDetails(tableData);
            return ResponseEntity.ok("保存成功！");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("保存失败！");
        }
    }

    @GetMapping("/downloadfiles/{fileId}")
    public ResponseEntity<Resource> downloadFileMysql(@PathVariable String fileId) {
        System.out.println("Viewing file with id: " + fileId);

        // 查询 FileInfo 实体
        FileInfo fileInfo = fileService.getBookById(Integer.parseInt(fileId));
        if (fileInfo == null) {
            return ResponseEntity.notFound().build();
        }

        // 获取 ISBN 字符串并输出
        String isbnString = fileInfo.getIsbn();
        System.out.println("ISBN String: " + isbnString);

        // 提取 ISBN 列表
        List<String> isbns = extractIsbnNumbers(isbnString);

        // 构造并检查每个 ISBN 的文件路径
        for (String isbn : isbns) {
            String possiblePath = fileInfo.getResourcesId() + "/" + isbn + ".pdf";
            try {
                Path filePath = this.fileStorageLocation.resolve(possiblePath).normalize();
                System.out.println("Checking path: " + filePath.toString());
                Resource resource = new UrlResource(filePath.toUri());

                if (resource.exists() || resource.isReadable()) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                }
            } catch (MalformedURLException ex) {
                System.err.println("Malformed URL: " + ex.getMessage());
            }
        }

        return ResponseEntity.notFound().build();
    }

    private List<String> extractIsbnNumbers(String input) {
        List<String> isbnList = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            isbnList.add(matcher.group());
        }
        return isbnList;
    }


    @GetMapping("/downloadpdfs/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id, @RequestParam String format) {
        System.out.println("Downloading file with id: " + id + " and format: " + format);
        try {
            Resource fileResource = getFileResourceByIdAndFormat(id, format);

            if (fileResource == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)  // 设置通用的MIME类型
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + "." + format + "\"")
                    .body(fileResource);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Resource getFileResourceByIdAndFormat(String id, String format) throws IOException {
        String directory;
        if ("pdf".equalsIgnoreCase(format)) {
            directory = PDF_DIRECTORY;

            com.example.lm.Model.FileInfo fileInfo = fileService.getFileById(Integer.parseInt(id));
            if (fileInfo == null) {
                return null;
            }

            String isbnString = fileInfo.getIsbn();
            List<String> isbns = extractIsbnNumbers(isbnString);

            for (String isbn : isbns) {
                String possiblePath = fileInfo.getResourcesId() + "/" + isbn + ".pdf";
                Path filePath = Paths.get(directory).resolve(possiblePath).normalize();
                java.io.File file = filePath.toFile();
                System.out.println(filePath);

                if (file.exists() && file.canRead()) {
                    return new InputStreamResource(new FileInputStream(file));
                }
            }

        } else if ("epub".equalsIgnoreCase(format)) {
            System.out.println("Downloading EPUB file...");
            directory = MARC_DIRECTORY;
            com.example.lm.Model.FileInfo fileInfo = fileService.getFileById(Integer.parseInt(id));
            if (fileInfo == null) {
                return null;
            }

            String title = "Corporate Sustainability  Shareholder Primacy Versus Stakeholder Primacy";
            Path filePath = Paths.get(directory, title + "." + format).normalize();
            System.out.println("Checking path: " + filePath.toString());
            java.io.File file = filePath.toFile();
            if (!file.exists()) {
                return null;
            }
            return new InputStreamResource(new FileInputStream(file));
        } else {
            return null; // 如果格式不支持，返回 null
        }
        return null; // 如果没有找到文件，返回 null
    }


    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        // 检查会话中是否有用户名
        return "index";  // 返回index视图
    }

    @GetMapping("/adminHome")
    public String adminHome(HttpSession session, Model model) {
        return "admin/adminHome";  // 返回adminHome视图
    }

    @GetMapping("/adminLogin")
    public String adminLogin(HttpSession session, Model model) {
        return "admin/adminLogin";  // 返回adminLogin视图
    }

    @GetMapping("/fileList")
    public String UserSearch(HttpSession session, Model model) {
        return "user/fileList";  // 返回adminLogin视图
    }


    @GetMapping("/pdf")
    public String viewPdf(@RequestParam("fileId") String fileId, Model model) {
        model.addAttribute("fileId", fileId);
        return "user/pdf";  // 返回 pdf.html 视图
    }

    @GetMapping("/getMarcNum")
    public ResponseEntity<?> checkNumOfMarc(@RequestParam("folderId") int folderId) {
        int numOfMarc = fileService.getMarcNum(folderId);
        return ResponseEntity.ok(numOfMarc);
    }

    @GetMapping("/getPDFNum")
    public ResponseEntity<?> checkNumOfPDF(@RequestParam("folderId") int folderId) {
        int pdfNum = fileService.getPDFNum(folderId);
        return ResponseEntity.ok(pdfNum);
    }

    @GetMapping("/getEPUBNum")
    public ResponseEntity<?> checkNumOfEPUB(@RequestParam("folderId") int folderId) {
        int epubNum = fileService.getEPUBNum(folderId);
        return ResponseEntity.ok(epubNum);
    }

    @GetMapping("/deleteFolder")
    public String deleteFolder(@RequestParam("folderId") int folderId) {
        resourcesLibService.deleteFolder(folderId);
        return "redirect:/resourcesLib";
    }

    @PostMapping("/saveExcel")
    public String saveExcel(@RequestParam("folderId") int folderId, @RequestParam("file") MultipartFile excel, String status, String view, String download, String borrow) throws IOException {
        int borrow_period = Integer.parseInt(borrow.replaceAll("[^0-9]", ""));
        fileService.saveExcel(folderId, excel, status, view, download, borrow_period);
        return "redirect:/resourcesLib";
    }

    @PostMapping("/uploadEPUB")
    public String handleEPUBUpload(@RequestParam int folderId, @RequestParam("files") List<MultipartFile> files, RedirectAttributes redirectAttributes) throws IOException {
        List<String> invalidFiles = fileService.saveEPUBs(folderId, files);
        if (!invalidFiles.isEmpty()) {
            redirectAttributes.addFlashAttribute("invalidFiles", invalidFiles);
            redirectAttributes.addFlashAttribute("folderIdWithInvalidFiles", folderId);
        }
        return "redirect:/resourcesLib";
    }


    @GetMapping("/getPublisher")
    public ResponseEntity<?> getPublisher() {
        List<String> publishers = fileService.getAllDistinctPublishers();
        return ResponseEntity.ok(publishers);
    }

    @GetMapping("/getSeries")
    public ResponseEntity<?> getSeries() {
        List<String> series = fileService.findAllDistinctSeries();
        return ResponseEntity.ok(series);
    }

    @GetMapping("/getYear")
    public ResponseEntity<?> getYear() {
        List<String> year = fileService.findAllDistinctCopyrightYear();
        return ResponseEntity.ok(year);
    }

    @GetMapping("/getDatabases")
    public ResponseEntity<?> getDatabases() {
        List<String> databases = resourcesLibService.getAllLib();
        return ResponseEntity.ok(databases);
    }

}

