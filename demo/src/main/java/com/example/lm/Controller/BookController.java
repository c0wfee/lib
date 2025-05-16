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
        return "bookDetail";
    }

    @Autowired
    private BookService bookService;

    @GetMapping("/search")
    public ResponseEntity<List<Book>> search(@RequestParam String q) {
        return ResponseEntity.ok(bookService.search(q));
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autoComplete(@RequestParam String prefix) {
        return ResponseEntity.ok(bookService.autoComplete(prefix));
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<String>> recommendKeywords() {
        return ResponseEntity.ok(bookService.recommendKeywords());
    }
}
