package com.example.lm.Dao;

import com.example.lm.Model.Borrow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Integer> {
    Borrow findByBorrowId(Integer borrowId);
    Optional<Borrow> findByBookId(Integer bookId);

    Borrow getBorrowByBorrowId(Integer borrow_id);

    List<Borrow> findByUsername(String username);

    @Query("SELECT b FROM Borrow b WHERE b.bookId = :bookID AND (b.status <> 'Returned' OR b.status IS NULL) ORDER BY b.loanEndTime DESC")
    Borrow getLoanEndTime(@Param("bookID") int bookID);


}
