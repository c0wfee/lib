package com.example.lm.Controller;

import com.example.lm.Dao.BorrowRepository;
import com.example.lm.Model.Borrow;
import com.example.lm.Model.FileInfo;
import com.example.lm.Service.BorrowService;
import com.example.lm.Service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Page<?> getAllBorrows(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return borrowService.getBorrowInfoForAdmin(page, size);
    }

    @GetMapping("/api/borrows/{username}")
    public List<Borrow> getBorrowsByUsername(@PathVariable String username) {
        return borrowRepository.findByUsername(username);
    }

    @GetMapping("/borrow/{fileId}")
    public ResponseEntity<Map<String, String>> getBorrowDetails(@PathVariable Integer fileId) {
        FileInfo file = fileService.getFileById(fileId);

        Map<String, String> response = new HashMap<>();
        if (file.getLoanLabel().equals("Returned")) {
            response.put("loanEndTime", "NULL");
        } else {
            String loanEndTime = borrowService.getLoanEndTime(fileId);
            response.put("loanEndTime", loanEndTime);
        }

        return ResponseEntity.ok(response);
    }


    @GetMapping("/borrow/count")
    public Map<String, Long> getBorrowCountForCurrentYear() {
        return borrowService.getBorrowCountByUserForCurrentYear();
    }

    @PostMapping("deleteBorrowInfo")
    public ResponseEntity<?> deleteBorrow(@RequestParam("borrow_id") int borrowID) {
       return ResponseEntity.ok(borrowService.deleteBorrowInfo(borrowID));
    }
}
