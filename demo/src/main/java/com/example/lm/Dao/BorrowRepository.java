package com.example.lm.Dao;

import com.example.lm.Model.Borrow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BorrowRepository extends JpaRepository<Borrow, Integer> {
    Borrow findByBorrowId(Integer borrowId);
    Optional<Borrow> findByBookId(Integer bookId);

    Borrow getBorrowByBookId(Integer bookId);

    List<Borrow> findByUsername(String username);


}
