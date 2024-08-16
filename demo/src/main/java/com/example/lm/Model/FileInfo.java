package com.example.lm.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "books")
public class FileInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "title")
    private String title;

    @Column(name = "alternative_title")
    private String alternativeTitle;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "book_databases_id")
    private int resourcesId;

    @Column(name = "authors")
    private String authors;

    @Column(name = "editors")
    private String editors;

    @Column(name = "series")
    private String series;

    @Column(name = "language")
    private String language;

    @Column(name = "isbn")
    private String isbn;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "published")
    private String published;

    @Column(name = "edition")
    private String edition;

    @Column(name = "copyright_year")
    private String copyrightYear;

    @Column(name = "copyright_declaration")
    private String copyrightDeclaration;

    @Column(name = "status")
    private String status;

    @Column(name = "url")
    private String url;

    @Column(name = "pages")
    private String pages;

    @Column(name = "subjects")
    private String subjects;

    @Column(name = "description")
    private String description;

    @Column(name = "chapters")
    private String chapters;

    @Column(name = "original_source")
    private String originalSource;

    @Column(name = "contributing_institution")
    private String contributingInstitution;

    @Column(name = "digitization_explanation")
    private String digitizationExplanation;

    @Column(name = "loan_label")
    private String loanLabel;

    @Column(name = "download")
    private String download;

    @Column(name = "view")
    private String view;

    @Column(name = "borrow_period")
    private int borrowPeriod;

    @Column(name = "downloadLink")
    private String downloadLink;

    @Column(name = "epub_path")
    private String epubPath;

    @Column(name = "database_name")
    private String databaseName;

    public String getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getResourcesId() {
        return resourcesId;
    }

    public void setResourcesId(int resourcesId) {
        this.resourcesId = resourcesId;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getEditors() {
        return editors;
    }

    public void setEditors(String editors) {
        this.editors = editors;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublished() {
        return published;
    }

    public void setPublished(String published) {
        this.published = published;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getCopyrightYear() {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear) {
        this.copyrightYear = copyrightYear;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public String getSubjects() {
        return subjects;
    }

    public void setSubjects(String subjects) {
        this.subjects = subjects;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChapters() {
        return chapters;
    }

    public void setChapters(String chapters) {
        this.chapters = chapters;
    }

    public String getAlternativeTitle() {
        return alternativeTitle;
    }

    public void setAlternativeTitle(String alternativeTitle) {
        this.alternativeTitle = alternativeTitle;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getCopyrightDeclaration() {
        return copyrightDeclaration;
    }

    public void setCopyrightDeclaration(String copyrightDeclaration) {
        this.copyrightDeclaration = copyrightDeclaration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalSource() {
        return originalSource;
    }

    public void setOriginalSource(String originalSource) {
        this.originalSource = originalSource;
    }

    public String getContributingInstitution() {
        return contributingInstitution;
    }

    public void setContributingInstitution(String contributingInstitution) {
        this.contributingInstitution = contributingInstitution;
    }

    public String getDigitizationExplanation() {
        return digitizationExplanation;
    }

    public void setDigitizationExplanation(String digitizationExplanation) {
        this.digitizationExplanation = digitizationExplanation;
    }

    public String getLoanLabel() {
        return loanLabel;
    }

    public void setLoanLabel(String loanLabel) {
        this.loanLabel = loanLabel;
    }

    public String getDownload() {
        return download;
    }

    public void setDownload(String download) {
        this.download = download;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public int getBorrowPeriod() {
        return borrowPeriod;
    }

    public void setBorrowPeriod(int borrowPeriod) {
        this.borrowPeriod = borrowPeriod;
    }

    public String getEpubPath() {
        return epubPath;
    }

    public void setEpubPath(String epubPath) {
        this.epubPath = epubPath;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "title='" + title + '\'' +
                ", authors='" + authors + '\'' +
                ", editors='" + editors + '\'' +
                ", series='" + series + '\'' +
                ", language='" + language + '\'' +
                ", isbn='" + isbn + '\'' +
                ", publisher='" + publisher + '\'' +
                ", published='" + published + '\'' +
                ", edition='" + edition + '\'' +
                ", copyrightYear='" + copyrightYear + '\'' +
                ", url='" + url + '\'' +
                ", pages='" + pages + '\'' +
                ", subjects='" + subjects + '\'' +
                ", description='" + description + '\'' +
                ", chapters='" + chapters + '\'' +
                '}';
    }
}
