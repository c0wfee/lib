package com.example.lm.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowDTO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "borrow_id")
    private Integer borrowId;
    private String username;
    private Integer bookId;
    private String loanStartTime;
    private String loanEndTime;
    private String bookTitle;
    private String returnedDate;
    private String status;
    private String ISBN;
    private String databaseName;
}
