package com.example.lm.Service;

import com.example.lm.Dao.BorrowRepository;
import com.example.lm.Dao.FileDao;
import com.example.lm.Dao.FileInfoDao;
import com.example.lm.Dao.PDFDao;
import com.example.lm.Model.File;
import com.example.lm.Model.FileInfo;
import com.example.lm.Model.PDFs;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class FileService {
    @Autowired
    private FileDao fileDao;

    @Autowired
    private  FileInfoDao fileInfoDao;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFSBucket gridFSBucket;

    @Autowired
    private PDFDao pdfDao;

    @Autowired
    private BorrowRepository borrowRepository;

    @Value("${MarcUploadPath}")
    private String MarcUploadPath;

    @Value("${PDFUploadPath}")
    private String PDFUploadPath;

    @Value("${EPUBUploadPath}")
    private String EPUBUploadPath;

    @Autowired
    private EntityManager entityManager;


    public List<String> savePDFs(int folderId, List<MultipartFile> files) throws IOException {
        List<String> invalidFiles = new ArrayList<>();

        String uploadPDFPath = PDFUploadPath + folderId;

        java.io.File uploadDir = new java.io.File(uploadPDFPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        for (MultipartFile file : files) {
            String PDFName = file.getOriginalFilename();
            if (PDFName != null && PDFName.toLowerCase().endsWith(".pdf")) {
                PDFName = PDFName.substring(0, PDFName.length() - 4);
            }

            if (fileInfoDao.findByResourcesIdAndIsbnContaining(folderId, PDFName).size() > 0) {
                java.io.File targetFile = new java.io.File(uploadDir.getAbsolutePath() + "/" + PDFName + ".pdf");
                try {
                    file.transferTo(targetFile);
                    PDFs pdf = new PDFs();
                    pdf.setName(file.getOriginalFilename());
                    pdf.setAddress(targetFile.getAbsolutePath());
                    pdf.setResourcesId(folderId);
                    pdfDao.save(pdf);

                    List<FileInfo> fileInfos = fileInfoDao.findByResourcesIdAndIsbnContaining(folderId, PDFName);
                    for (FileInfo fileInfo : fileInfos) {
                        fileInfo.setDownloadLink(Integer.toString(pdf.getId()));
                        fileInfoDao.save(fileInfo);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                invalidFiles.add(PDFName);
            }
        }
        return invalidFiles;
    }

    public List<String> saveEPUBs(int folderId, List<MultipartFile> files) throws IOException {
        List<String> invalidFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            String EPUBName = file.getOriginalFilename();
            if (EPUBName != null && EPUBName.toLowerCase().endsWith(".epub")) {
                EPUBName = EPUBName.substring(0, EPUBName.length() - 5);
            }

            if (fileInfoDao.findByResourcesIdAndIsbnContaining(folderId, EPUBName).size() > 0) {
                String uploadPDFPath = EPUBUploadPath + folderId;
                System.out.println(uploadPDFPath);
                java.io.File uploadFile = new java.io.File(uploadPDFPath);
                if (!uploadFile.exists()) {
                    uploadFile.mkdirs();
                }
                java.io.File targetFile = new java.io.File(uploadFile.getAbsolutePath() + "/" + EPUBName + ".epub");
                try {
                    file.transferTo(targetFile);
                    PDFs pdf = new PDFs();
                    pdf.setName(file.getOriginalFilename());
                    pdf.setAddress(targetFile.getAbsolutePath());
                    pdf.setResourcesId(folderId);
                    pdfDao.save(pdf);

                    // Update the FileInfo table with the download link
                    List<FileInfo> fileInfos = fileInfoDao.findByResourcesIdAndIsbnContaining(folderId, EPUBName);
                    for (FileInfo fileInfo : fileInfos) {
                      
                        fileInfo.setEpubPath(Integer.toString(pdf.getId()));
                        fileInfoDao.save(fileInfo);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                invalidFiles.add(EPUBName);
            }
        }
        return invalidFiles;
    }


    private String extractPdfText(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    public List<GridFSFile> listFiles(String keyword) {
        Query query = new Query();
        if (keyword != null && !keyword.isEmpty()) {
            query.addCriteria(Criteria.where("filename").regex(keyword, "i"));
        }

        GridFSFindIterable iterable = gridFsTemplate.find(query);
        List<GridFSFile> files = new ArrayList<>();
        iterable.forEach((Consumer<? super GridFSFile>) files::add);
        return files;
    }
    public GridFSFile getFileById(String id) {
        return gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id)));
    }

    // 删除文件
    public void deleteFileById(String id) {
        GridFSFile file = getFileById(id);
        if (file != null) {
            gridFSBucket.delete(file.getObjectId());
            deleteFileById(id);
        }
    }

    public void uploadMARCFile(int folderId, MultipartFile marc, String status, String view, String download, int borrow_period){
        if(marc != null) {
            String fileName = marc.getOriginalFilename();
            String uploadMarcPath = MarcUploadPath;
            java.io.File uploadFile = new java.io.File(uploadMarcPath);
            if (!uploadFile.exists()) {
                uploadFile.mkdirs();
            }
            java.io.File targetFile = new java.io.File(uploadFile.getAbsolutePath() + "/" + fileName);
            try {
                marc.transferTo(targetFile);
                System.out.println(targetFile.getAbsoluteFile());
                saveMarcData(folderId, targetFile, status, view, download, borrow_period);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     *
     *
     */
    private void saveMarcData(int folderId, java.io.File marc, String status, String view, String download, int borrow_period) {
        try {
            String marcFilePath = marc.getAbsolutePath();
            InputStream inputStream = new FileInputStream(marcFilePath);
            MarcReader reader = new MarcStreamReader(inputStream);
            String fieldNum245 = "245";

            int bookCount = 0;
            while (reader.hasNext()) {
                Record record =  reader.next();
                if (hasField(record, fieldNum245)) {
                    bookCount++;
                }

                FileInfo fileInfo = new FileInfo();
                fileInfo.setTitle(getFieldData(record, "245", 'a', 'b'));
                fileInfo.setAuthors(getFieldData(record, new String[]{"100", "110"}, 'a'));
                fileInfo.setEditors(getFieldData(record, new String[]{"700", "710"}, 'a'));
                fileInfo.setSeries(getFieldData(record, "490", 'a'));
                fileInfo.setLanguage(getFieldData(record, "041", 'a'));
                fileInfo.setIsbn(getFieldData(record, "020", 'a', 'z'));
                fileInfo.setPublisher(getFieldData(record, new String[]{"264", "260"}, 'b'));
                fileInfo.setPublished(getFieldData(record, new String[]{"264", "260"}, 'c'));
                fileInfo.setEdition(getFieldData(record, "250", 'a'));
                fileInfo.setCopyrightYear(getFieldData(record, new String[]{"264_4", "260"}, 'c'));
                fileInfo.setUrl(getFieldData(record, "856", 'u'));
                fileInfo.setPages(getFieldData(record, "300", 'a'));
                fileInfo.setSubjects(getFieldData(record, "650", 'a'));
                fileInfo.setDescription(getFieldData(record, "520", 'a'));
                fileInfo.setChapters(getFieldData(record, "505", 'a'));
                fileInfo.setResourcesId(folderId);
                fileInfo.setStatus(status);
                fileInfo.setView(view);
                fileInfo.setDownload(download);
                fileInfo.setBorrowPeriod(borrow_period);
                fileInfoDao.save(fileInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean hasField(Record record, String fieldNum) {
        for (VariableField field : record.getVariableFields()) {
            if (field.getTag().equals(fieldNum)) {
                return true;
            }
        }
        return false;
    }

    private static String getFieldData(Record record, String fieldNum, char... subfieldCodes) {
        List<VariableField> fields = record.getVariableFields(fieldNum);
        StringBuilder data = new StringBuilder();

        for (VariableField field : fields) {
            if (field instanceof DataField) {
                DataField dataField = (DataField) field;
                for (char subfieldCode : subfieldCodes) {
                    Subfield subfield = dataField.getSubfield(subfieldCode);
                    if (subfield != null) {
                        if (data.length() > 0) {
                            data.append(" ");
                        }
                        String subfieldData = subfield.getData();
                        // 仅对非856字段进行字符替换和修剪
                        if (!"856".equals(fieldNum)) {
                            subfieldData = subfieldData.replaceAll("[/,:]", "").trim();
                        } else {
                            subfieldData = subfieldData.trim();
                        }
                        data.append(subfieldData);
                    }
                }
            }
        }
        return data.toString();
    }


    private static String getFieldData(Record record, String[] fieldNums, char... subfieldCodes) {
        StringBuilder data = new StringBuilder();

        for (String fieldNum : fieldNums) {
            String fieldData = getFieldData(record, fieldNum, subfieldCodes);
            if (fieldNums.length == 2 && fieldNums[0].equals("264") && fieldNums[1].equals("260") && subfieldCodes.length == 1 && subfieldCodes[0] == 'c') {
                // 只保留数字
                fieldData = fieldData.replaceAll("[^0-9]", "");
            }
            data.append(fieldData);
        }
        return data.toString();
    }


    public List<File> getPDFsByLib(int resourcesID) {
        return fileDao.findFilenamesByResourcesId(resourcesID);
    }

    public void savePDF(FileInfo pdf){
        fileInfoDao.save(pdf);
        return;
    }

    public List<FileInfo> getMarcDetailByID(int resourcesID) {
        return fileInfoDao.findByResourcesId(resourcesID);
    }

    public FileInfo getFileById(int bookID) {
        return fileInfoDao.findById(bookID);
    }

    public List<FileInfo> getListPDFs(Integer databaseId) {return fileInfoDao.findPDFs(databaseId);}

    public List<FileInfo> getListEpubs(Integer databaseId) {return fileInfoDao.findEpubs(databaseId);}
    public Page<FileInfo> keywordSearch(String keyword, Pageable pageable) {
        String searchPattern = "%" + keyword + "%"; // 在关键字前后加上百分号
        String jpql = "SELECT f FROM FileInfo f WHERE f.title LIKE :keyword";

        TypedQuery<FileInfo> query = entityManager.createQuery(jpql, FileInfo.class);
        query.setParameter("keyword", searchPattern);

        // 获取总记录数
        String countJpql = "SELECT COUNT(f) FROM FileInfo f WHERE f.title LIKE :keyword";
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        countQuery.setParameter("keyword", searchPattern);
        long total = countQuery.getSingleResult();

        // 设置分页
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<FileInfo> fileList = query.getResultList();

        return new PageImpl<>(fileList, pageable, total);
    }



    public Page<FileInfo> advancedSearch(String keyword, List<String> series, List<String> publisher, List<String> subjects, List<String> database, Integer publishedFrom, Integer publishedTo, Integer publishedYear, Pageable pageable) {
        String searchPattern = "%" + keyword + "%"; // 在关键字前后加上百分号
        String jpql = "SELECT f FROM FileInfo f WHERE f.status = 'published'";
        String countJpql = "SELECT COUNT(f) FROM FileInfo f WHERE f.status = 'published'";
        List<Integer> databaseIds = null;

        if (database != null && !database.isEmpty()) {
            // 查询 resources_lib 表，获取对应的 name 列值
            String resourcesJpql = "SELECT r.id FROM ResourcesLib r WHERE r.name IN :databaseNames";
            TypedQuery<Integer> resourcesQuery = entityManager.createQuery(resourcesJpql, Integer.class);
            resourcesQuery.setParameter("databaseNames", database);
            databaseIds = resourcesQuery.getResultList();
            System.out.println(databaseIds);

            // 将获取到的 id 列值存储到 database 数组中
            if (databaseIds != null && !databaseIds.isEmpty()) {
                jpql += " AND f.resourcesId IN :databaseIds";
                countJpql += " AND f.resourcesId IN :databaseIds";
            }
        }

        if (keyword != null && !keyword.isEmpty()) {
            jpql += " AND f.title LIKE :keyword";
            countJpql += " AND f.title LIKE :keyword";
        }

        if (series != null && !series.isEmpty()) {
            jpql += " AND f.series IN :series";
            countJpql += " AND f.series IN :series";
        }

        if (publisher != null && !publisher.isEmpty()) {
            jpql += " AND f.publisher IN :publisher";
            countJpql += " AND f.publisher IN :publisher";
        }

        if (subjects != null && !subjects.isEmpty()) {
            jpql += " AND f.subjects IN :subjects";
            countJpql += " AND f.subjects IN :subjects";
        }

        if (publishedFrom != null) {
            jpql += " AND f.published >= :publishedFrom";
            countJpql += " AND f.published >= :publishedFrom";
        }

        if (publishedTo != null) {
            jpql += " AND f.published <= :publishedTo";
            countJpql += " AND f.published <= :publishedTo";
        }

        if (publishedYear != null) {
            jpql += " AND f.published = :publishedYear";
            countJpql += " AND f.published = :publishedYear";
        }

        TypedQuery<FileInfo> query = entityManager.createQuery(jpql, FileInfo.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        if (keyword != null && !keyword.isEmpty()) {
            query.setParameter("keyword", searchPattern);
            countQuery.setParameter("keyword", searchPattern);
        }

        if (series != null && !series.isEmpty()) {
            query.setParameter("series", series);
            countQuery.setParameter("series", series);
        }

        if (publisher != null && !publisher.isEmpty()) {
            query.setParameter("publisher", publisher);
            countQuery.setParameter("publisher", publisher);
        }

        if (subjects != null && !subjects.isEmpty()) {
            query.setParameter("subjects", subjects);
            countQuery.setParameter("subjects", subjects);
        }

        if (database != null && !database.isEmpty()) {
            System.out.println(databaseIds);
            query.setParameter("databaseIds", databaseIds);
            countQuery.setParameter("databaseIds", databaseIds);
        }

        if (publishedFrom != null) {
            query.setParameter("publishedFrom", String.valueOf(publishedFrom));
            countQuery.setParameter("publishedFrom", String.valueOf(publishedFrom));
        }

        if (publishedTo != null) {
            query.setParameter("publishedTo", String.valueOf(publishedTo));
            countQuery.setParameter("publishedTo", String.valueOf(publishedTo));
        }

        if (publishedYear != null) {
            query.setParameter("publishedYear", String.valueOf(publishedYear));
            countQuery.setParameter("publishedYear", String.valueOf(publishedYear));
        }

        // 打印调试信息
        System.out.println("JPQL Query: " + jpql);
        System.out.println("Keyword: " + searchPattern);
        System.out.println("Series: " + series);
        System.out.println("Publisher: " + publisher);
        System.out.println("Subjects: " + subjects);
        System.out.println("Database: " + database);
        System.out.println("PublishedFrom: " + publishedFrom);
        System.out.println("PublishedTo: " + publishedTo);
        System.out.println("PublishedYear: " + publishedYear);

        // 获取总记录数
        long total = countQuery.getSingleResult();
        System.out.println(total);

        // 设置分页
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<FileInfo> fileList = query.getResultList();

        return new PageImpl<>(fileList, pageable, total);
    }





    public void saveMarcDetails(List<Map<String, String>> tableData) {
        for (Map<String, String> row : tableData) {
            FileInfo marcDetail = new FileInfo();
            marcDetail.setId(Integer.parseInt(row.get("id")));
            marcDetail.setTitle(row.get("title"));
            marcDetail.setAlternativeTitle(row.get("alternativeTitle"));
            marcDetail.setSourceType(row.get("sourceType"));
            marcDetail.setResourcesId(Integer.parseInt(row.get("resourcesId")));
            marcDetail.setAuthors(row.get("authors"));
            marcDetail.setEditors(row.get("editors"));
            marcDetail.setSeries(row.get("series"));
            marcDetail.setLanguage(row.get("language"));
            marcDetail.setIsbn(row.get("isbn"));
            marcDetail.setPublisher(row.get("publisher"));
            marcDetail.setPublished(row.get("published"));
            marcDetail.setEdition(row.get("edition"));
            marcDetail.setCopyrightYear(row.get("copyrightYear"));
            marcDetail.setCopyrightDeclaration(row.get("copyrightDeclaration"));
            marcDetail.setStatus(row.get("status"));
            marcDetail.setUrl(row.get("url"));
            marcDetail.setPages(row.get("pages"));
            marcDetail.setSubjects(row.get("subjects"));
            marcDetail.setDescription(row.get("description"));
            marcDetail.setChapters(row.get("chapters"));
            marcDetail.setOriginalSource(row.get("originalSource"));
            marcDetail.setContributingInstitution(row.get("contributingInstitution"));
            marcDetail.setDigitizationExplanation(row.get("digitizationExplanation"));
            marcDetail.setLoanLabel(row.get("loanLabel"));

            fileInfoDao.save(marcDetail);
        }
    }

    public List<FileInfo> searchByTitle(String title) {
        return fileInfoDao.findByTitleContaining(title);
    }


    public void deleteBook(int id) {
        fileInfoDao.deleteById(id);
    }

    public void updateBook(FileInfo fileInfo) {
        // 检查是否存在
        if (fileInfoDao.existsById(fileInfo.getId())) {
            // 更新
            fileInfoDao.save(fileInfo);
        } else {
            throw new EntityNotFoundException("Book with ID " + fileInfo.getId() + " not found.");
        }
    }

    @Transactional
    public void updateStatus(int id, String newStatus) {
        fileInfoDao.updateStatusById(id, newStatus);
    }

    @Transactional
    public void updateLoan(int id, String newLoan) {
        fileInfoDao.updateLoanById(id, newLoan);
    }
    @Transactional
    public void updateField(int id, String editType, String editField) {
        FileInfo book = fileInfoDao.findById(id);
        if (book != null) {
            if ("status".equals(editType)) {
                book.setStatus(editField);
            } else if ("loan".equals(editType)) {
                book.setBorrowPeriod(Integer.parseInt(editField));
            }else if ("download".equals(editType)) {
                book.setDownload(editField);
            }else if ("view".equals(editType)) {
                book.setView(editField);
            }
            fileInfoDao.save(book);
        } else {
            throw new RuntimeException("Book not found");
        }
    }

    public List<FileInfo> searchBooks(String title, String isbn, String alternativeTitle, String author,
                                      String status, String publisher, String sourceType,
                                      String language, String published, Integer databaseId) {
        // 处理空字符串，将其转换为 null
        title = (title != null && title.isEmpty()) ? null : title;
        isbn = (isbn != null && isbn.isEmpty()) ? null : isbn;
        alternativeTitle = (alternativeTitle != null && alternativeTitle.isEmpty()) ? null : alternativeTitle;
        author = (author != null && author.isEmpty()) ? null : author;
        status = (status != null && status.isEmpty()) ? null : status;
        publisher = (publisher != null && publisher.isEmpty()) ? null : publisher;
        sourceType = (sourceType != null && sourceType.isEmpty()) ? null : sourceType;
        language = (language != null && language.isEmpty()) ? null : language;
        published = (published != null && published.isEmpty()) ? null : published;

        return fileInfoDao.searchBooks(title, isbn, alternativeTitle, author, status, publisher, sourceType, language, published, databaseId);
    }

    public FileInfo getBookById(Integer id) {
        return fileInfoDao.findById(id).orElse(null);
    }
    public List<String> getAllDistinctPublishers() {
        return fileInfoDao.findAllDistinctPublishers();
    }

    public List<String> getAllDistinctPublished() {
        return fileInfoDao.findAllDistinctPublished();
    }

    public List<String> getAllDistinctSourceType(){return fileInfoDao.findAllDistinctSourceType();}

    public List<String> getAllDistinctLanguage(){return fileInfoDao.findAllDistinctLanguage();}

    public List<String> getAllDistinctStatus(){return fileInfoDao.findAllDistinctStatus();}

    public List<Integer> getAllDistinctDatabaseId(){return fileInfoDao.findAllDistinctDatabaseId();}

    public boolean isBookBorrowed(Integer bookId) {
        return borrowRepository.findByBookId(bookId).isPresent();
    }

    public int getBookPeriod(int bookId) {
        FileInfo fl = fileInfoDao.findById(bookId);
        return fl.getBorrowPeriod();
    }

    public int getMarcNum(int folderId) {
        return fileInfoDao.findByResourcesId(folderId).size();
    }

    public int getPDFNum(int folderId) {
        return fileInfoDao.getPDFNum(folderId).size();
    }

    public int getEPUBNum(int folderId) {
        return fileInfoDao.getEPUBNum(folderId).size();
    }

    public void saveExcel(int folderId, MultipartFile excel, String status, String view, String download, int borrow_period) throws IOException {
        try (InputStream inputStream = excel.getInputStream()) {
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);
            int sheetNum = xssfWorkbook.getNumberOfSheets();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            for (int i = 0; i < sheetNum; i++) {
                XSSFSheet sheet = xssfWorkbook.getSheetAt(i);
                int maxRow = sheet.getLastRowNum();

                // 读取表头行
                Map<String, Integer> headerMap = new HashMap<>();
                for (int col = 0; col < sheet.getRow(0).getLastCellNum(); col++) {
                    headerMap.put(sheet.getRow(0).getCell(col).getStringCellValue(), col);
                }

                // 遍历数据行
                for (int row = 1; row <= maxRow; row++) {
                    if (sheet.getRow(row) == null) {
                        continue; // 跳过空行
                    }

                    FileInfo bookMetadata = new FileInfo();

                    for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                        String columnName = entry.getKey();
                        int colIndex = entry.getValue();
                        Cell cell = sheet.getRow(row).getCell(colIndex);

                        if (cell != null) {
                            bookMetadata.setStatus(status);
                            bookMetadata.setView(view);
                            bookMetadata.setDownload(download);
                            bookMetadata.setBorrowPeriod(borrow_period);
                            bookMetadata.setResourcesId(folderId);

                            String cellValue = null;
                            if (cell.getCellType() == CellType.NUMERIC) {
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    cellValue = dateFormat.format(cell.getDateCellValue());
                                } else {
                                    // 检查是否为整数
                                    double numericValue = cell.getNumericCellValue();
                                    if (numericValue == (int) numericValue) {
                                        cellValue = Integer.toString((int) numericValue);
                                    } else {
                                        cellValue = BigDecimal.valueOf(numericValue).toPlainString();
                                    }
                                }
                            } else {
                                cellValue = cell.toString();
                            }

                            switch (columnName) {
                                case "Title":
                                    bookMetadata.setTitle(cellValue);
                                    break;
                                case "Alternate Title":
                                    bookMetadata.setAlternativeTitle(cellValue);
                                    break;
                                case "Source Type":
                                    bookMetadata.setSourceType(cellValue);
                                    break;
                                case "Authors":
                                    bookMetadata.setAuthors(cellValue);
                                    break;
                                case "Editors":
                                    bookMetadata.setEditors(cellValue);
                                    break;
                                case "Series":
                                    bookMetadata.setSeries(cellValue);
                                    break;
                                case "Language":
                                    bookMetadata.setLanguage(cellValue);
                                    break;
                                case "ISBN":
                                    bookMetadata.setIsbn(cellValue);
                                    break;
                                case "Publisher":
                                    bookMetadata.setPublisher(cellValue);
                                    break;
                                case "Published":
                                    bookMetadata.setPublished(cellValue);
                                    break;
                                case "Edition":
                                    bookMetadata.setEdition(cellValue);
                                    break;
                                case "Copyright Year":
                                    bookMetadata.setCopyrightYear(cellValue);
                                    break;
                                case "Copyright Declaration":
                                    bookMetadata.setCopyrightDeclaration(cellValue);
                                    break;
                                case "status":
                                    bookMetadata.setStatus(cellValue);
                                    break;
                                case "URL":
                                    bookMetadata.setUrl(cellValue);
                                    break;
                                case "Pages":
                                    bookMetadata.setPages(cellValue);
                                    break;
                                case "Subjects":
                                    bookMetadata.setSubjects(cellValue);
                                    break;
                                case "Description":
                                    bookMetadata.setDescription(cellValue);
                                    break;
                                case "Chapters":
                                    bookMetadata.setChapters(cellValue);
                                    break;
                                case "Original Source":
                                    bookMetadata.setOriginalSource(cellValue);
                                    break;
                                case "Contributing Institution":
                                    bookMetadata.setContributingInstitution(cellValue);
                                    break;
                                case "Digitization Explanation":
                                    bookMetadata.setDigitizationExplanation(cellValue);
                                    break;
                                case "borrow_period":
                                    if (cell.getCellType() == CellType.NUMERIC) {
                                        bookMetadata.setBorrowPeriod((int) cell.getNumericCellValue());
                                    } else {
                                        bookMetadata.setBorrowPeriod(Integer.parseInt(cellValue));
                                    }
                                    break;
                            }
                        }
                    }
                    // 保存到数据库
                    fileInfoDao.save(bookMetadata);
                }
            }
        }
    }

    public List<FileInfo> findByAllKinds(String title, String isbn, String alternativeTitle, String author, String status, String publisher, String sourceType, String language, String published, Integer databaseId) {
        title = (title != null && title.isEmpty()) ? null : title;
        isbn = (isbn != null && isbn.isEmpty()) ? null : isbn;
        alternativeTitle = (alternativeTitle != null && alternativeTitle.isEmpty()) ? null : alternativeTitle;
        author = (author != null && author.isEmpty()) ? null : author;
        status = (status != null && status.isEmpty()) ? null : status;
        publisher = (publisher != null && publisher.isEmpty()) ? null : publisher;
        sourceType = (sourceType != null && sourceType.isEmpty()) ? null : sourceType;
        language = (language != null && language.isEmpty()) ? null : language;
        published = (published != null && published.isEmpty()) ? null : published;

        return fileInfoDao.findByAllKinds(title, isbn, alternativeTitle, author, status, publisher, sourceType, language, published, databaseId);

    }

    @Transactional
    public boolean deletePdfById(String pdfID, Integer databaseId) {
        Optional<PDFs> fileOptional = pdfDao.findById(Integer.valueOf(pdfID));
        if (fileOptional.isPresent()) {
            PDFs pdf = fileOptional.get();
            String fileName = pdf.getName();
            pdfDao.delete(pdf);
            // 更新 book 表中相应的 downloadLink 字段为空
            fileInfoDao.updateDownloadLinkToNull(pdfID);

            // 删除文件系统中的文件
            try {
                Path filePath = Paths.get(PDFUploadPath+ databaseId +"/" + fileName);
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteEpubById(String pdfID, Integer databaseId) {
        Optional<PDFs> fileOptional = pdfDao.findById(Integer.valueOf(pdfID));
        if (fileOptional.isPresent()) {
            PDFs pdf = fileOptional.get();
            String fileName = pdf.getName();
            pdfDao.delete(pdf);
            // 更新 book 表中相应的 downloadLink 字段为空
            fileInfoDao.updateEpubPathToNull(pdfID);

            // 删除文件系统中的文件
            try {
                Path filePath = Paths.get(EPUBUploadPath+ databaseId +"/" + fileName);
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    public Map<String, String> getPdfNamesByLinks(List<String> links) {
        List<Integer> ids = links.stream().map(Integer::parseInt).collect(Collectors.toList());
        List<PDFs> pdfList = pdfDao.findByIdIn(ids);
        return pdfList.stream().collect(Collectors.toMap(pdf -> String.valueOf(pdf.getId()), PDFs::getName));
    }

//    public Map<String, String> getepubNamesByLinks(List<String> links) {
//        List<Integer> ids = links.stream().map(Integer::parseInt).collect(Collectors.toList());
//        List<PDFs> pdfList = pdfDao.findByIdIn(ids);
//        return pdfList.stream().collect(Collectors.toMap(pdf -> String.valueOf(pdf.getId()), PDFs::getName));
//    }
}