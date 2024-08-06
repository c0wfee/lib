package com.example.lm.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BookController {

    @GetMapping("/bookDetail")
    public String bookDetail(@RequestParam(name = "id") String id, Model model) {
        System.out.println("id: " + id);
        model.addAttribute("fileId", id);
        return "user/bookDetail";
    }

}
