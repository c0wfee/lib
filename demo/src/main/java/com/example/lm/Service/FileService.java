package com.example.lm.Service;

import com.example.lm.Dao.FileDao;
import com.example.lm.Dao.FileInfoDao;
import com.example.lm.Dao.PDFService;
import com.example.lm.Model.File;
import com.example.lm.Model.FileInfo;
import com.example.lm.Model.PDFs;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class FileService {
    @Autowired
    private FileDao fileDao;

    @Autowired
    private FileInfoDao fileInfoDao;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFSBucket gridFSBucket;

    @Autowired
    private PDFService pdfService;

    @Value("${MarcUploadPath}")
    private String MarcUploadPath;

    @Value("${PDFUploadPath}")
    private String PDFUploadPath;

    @Autowired
    private EntityManager entityManager;


    public List<String> savePDFs(int folderId, List<MultipartFile> files) throws IOException {
        List<String> invalidFiles = new ArrayList<>();

        for (MultipartFile file : files) {

            String PDFName = file.getOriginalFilename();
            if (PDFName != null && PDFName.toLowerCase().endsWith(".pdf")) {
                PDFName = PDFName.substring(0, PDFName.length() - 4);
            }

            if (fileInfoDao.findByResourcesIdAndIsbnContaining(folderId, PDFName).size() > 0) {
                /*ObjectId gridFsId = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
                String content = extractPdfText(file.getInputStream());

                File newFile = new File();
                newFile.setFilename(file.getOriginalFilename());
                newFile.setContentType(file.getContentType());
                newFile.setSize(file.getSize());
                newFile.setGridFsId(gridFsId.toString());
                newFile.setContent(content);
                newFile.setResourcesId(folderId);
                fileDao.save(newFile);*/
                String uploadPDFPath = PDFUploadPath;
                java.io.File uploadFile = new java.io.File(uploadPDFPath);
                if (!uploadFile.exists()) {
                    uploadFile.mkdirs();
                }
                java.io.File targetFile = new java.io.File(uploadFile.getAbsolutePath() + "/" + PDFName);
                try {
                    file.transferTo(targetFile);
                    PDFs pdf = new PDFs();
                    pdf.setName(file.getOriginalFilename());
                    pdf.setAddress(targetFile.getAbsolutePath());
                    pdf.setResourcesId(folderId);
                    pdfService.save(pdf);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                invalidFiles.add(PDFName);
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

    public void uploadMARCFile(int folderId, MultipartFile marc){
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
                saveMarcData(folderId, targetFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     * displayCode: 0: Hidden; 1: Published
     *
     */
    private void saveMarcData(int folderId, java.io.File marc) {
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
                fileInfo.setDisplay(0);
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
            data.append(getFieldData(record, fieldNum, subfieldCodes));
        }
        return data.toString();
    }

    public List<File> getPDFsByLib(int resourcesID) {
        List<File> PDFsList = fileDao.findFilenamesByResourcesId(resourcesID);
        return PDFsList;
    }

    public List<FileInfo> getMarcDetailByID(int resourcesID) {
        return fileInfoDao.findByResourcesId(resourcesID);
    }

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
}