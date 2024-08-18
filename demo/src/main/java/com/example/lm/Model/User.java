package com.example.lm.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer id;

    @Column(nullable = false, unique = true)
    private String username;

    private String password;

    @Column(name = "unban_time")
    private String unbanTime;

    @Column(name = "ban_time")
    private String banTime;

    @Column(name = "status")
    private String status;

    @Column(name = "total_borrowings")
    private int totalBorrowings;
}
