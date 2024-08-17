package com.example.lm.Controller;

import com.example.lm.Dao.BorrowRepository;
import com.example.lm.Model.Borrow;
import com.example.lm.Model.FileInfo;
import com.example.lm.Service.BorrowService;
import com.example.lm.Service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class BorrowApiController {

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private BorrowService borrowService;

    @GetMapping("/api/borrows")
    public List<Borrow> getAllBorrows() {
        return borrowRepository.findAll();
    }

    @GetMapping("/api/borrows/{username}")
    public List<Borrow> getBorrowsByUsername(@PathVariable String username) {
        return borrowRepository.findByUsername(username);
    }

    @GetMapping("/borrow/{fileId}")
    public ResponseEntity<?> getBorrowDetails(@PathVariable Integer fileId) {
        FileInfo file = fileService.getFileById(fileId);

        if (file.getLoanLabel().equals("Returned")) {
            return ResponseEntity.ok("NULL");
        }
        else {
            return ResponseEntity.ok(borrowService.getLoanEndTime(fileId));
        }

    }


    @PostMapping("deleteBorrowInfo")
    public ResponseEntity<?> deleteBorrow(@RequestParam("borrow_id") int borrowID) {
       return ResponseEntity.ok(borrowService.deleteBorrowInfo(borrowID));
    }
}
