package com.example.lm.utils;

import com.example.lm.Model.YearRange;

import java.util.List;

public class FilterData {
    private String keyword;
    private List<String> series;
    private List<String> publisher;
    private List<String> subject;
    private List<String> database;
    private YearRange yearRange;
    private Integer publishedYear;
    private int page;
    private int size;

    public String getKeyword() {
        return keyword;
    }

    @Override
    public String toString() {
        return "FilterData{" +
                "keyword='" + keyword + '\'' +
                ", series=" + series +
                ", publisher=" + publisher +
                ", subject=" + subject +
                ", database=" + database +
                ", publishedYear=" + publishedYear +
                ", page=" + page +
                ", size=" + size +
                '}';
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<String> getSeries() {
        return series;
    }

    public void setSeries(List<String> series) {
        this.series = series;
    }

    public List<String> getPublisher() {
        return publisher;
    }

    public void setPublisher(List<String> publisher) {
        this.publisher = publisher;
    }

    public List<String> getDatabase() {
        return database;
    }

    public void setDatabase(List<String> database) {
        this.database = database;
    }

    public List<String> getSubject() {
        return subject;
    }

    public void setSubject(List<String> subject) {
        this.subject = subject;
    }

    public YearRange getYearRange() {
        return yearRange;
    }

    public void setYearRange(YearRange yearRange) {
        this.yearRange = yearRange;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public void setPublishedYear(Integer publishedYear) {
        this.publishedYear = publishedYear;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
// Getters and Setters
}

