package com.example.lm.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BorrowViewController {

    @GetMapping("/adminBorrow")
    public String getBorrowedBooksPage() {
        return "admin/adminBorrow";
    }

    @GetMapping("/userHome")
    public String getUserHomePage() {
        return "user/userHome"; // This should match the name of your HTML file without the .html extension
    }
}
