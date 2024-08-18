package com.example.lm.Controller;

import com.example.lm.Model.FileInfo;
import com.example.lm.Model.User;
import com.example.lm.Model.UserInfo;
import com.example.lm.Service.TokenService;
import com.example.lm.Service.UserService;
import com.example.lm.utils.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @GetMapping("/searchUsers")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        List<User> users;
        Pageable paging = PageRequest.of(page - 1, size);

        Page<User> pageUsers;
        if (keyword == null || keyword.isEmpty()) {
            pageUsers = userService.getAllUsers2(paging);
        } else {
            pageUsers = userService.getUsersByKeyword2(keyword, paging);
        }

        users = pageUsers.getContent();
        Map<String, Object> response = new HashMap<>();
        response.put("content", users);
        response.put("currentPage", pageUsers.getNumber() + 1);
        response.put("totalItems", pageUsers.getTotalElements());
        response.put("totalPages", pageUsers.getTotalPages());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @GetMapping("/getUsers")
    public ResponseEntity<List<User>> getUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }



    @GetMapping("/log_in")
    public String index(Model model) {
        String message = (String) model.asMap().get("message");
        if (message != null) {
            model.addAttribute("message", message);
        }
        return "login";
    }


    @PostMapping("/addBookToCollection")
    public String addBookToCollection(HttpSession session, @RequestParam String bookisbn, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            // 如果用户名不存在，跳转到登录页面
            return "redirect:/adminHome";
        }
        try {
            userService.addBookToUserCollection(username, bookisbn);
            model.addAttribute("message", "Add collection successful!");
            return "lists";
        } catch (IllegalArgumentException e) {
            model.addAttribute("message", e.getMessage());
            return "lists";
        }
    }

    @PostMapping("/listCollection")
    public String listCollection(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/log_in";
        }
        Set<FileInfo> books = userService.listCollect(username);
        model.addAttribute("Collections", books);
        return "lists";
    }

    @PostMapping("/removeCollection")
    public String removeCollection(HttpSession session, Model model, @RequestParam String bookisbn) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/log_in";
        }
        try {
            userService.removeBookCollection(username, bookisbn);
            model.addAttribute("message", "Remove collection successful!");
            return "lists";
        } catch (IllegalArgumentException e) {
            model.addAttribute("message", e.getMessage());
            return "lists";
        }
    }

    @PostMapping("/api/user/login")
    public Result login(@RequestBody String json) {
        System.out.println("Received JSON: " + json);

        // 解析 JSON
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = null;
        try {
            map = mapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 获取用户名和密码
        String username = map.get("username");
        String password = map.get("password");

        // 验证用户名和密码
        User user = null;
        try {
            user = userService.authenticate(username);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("User is banned until") || e.getMessage().contains("User not found")) {
                return Result.error().data("error", e.getMessage());
            }
            return Result.error().data("error", "Authentication failed.");
        }

        if (user != null) {
            if (user.getPassword().equals(password)) {
                Map<String, String> message = new HashMap<>();
                String token = tokenService.generateTempToken(String.valueOf(user.getId()));
                System.out.println("success token: " + token);
                message.put("token", token);
                message.put("id", String.valueOf(user.getId()));
                message.put("name", user.getUsername());
                return Result.ok().data(message);
            } else {
                return Result.error().data("error", "password error");
            }
        } else {
            return Result.error().data("error", "user not found");
        }
    }

    @GetMapping("/api/users/search")
    public Page<User> searchUsers(Pageable pageable) {
        System.out.println("Fetching users with pageable: " + pageable);

        // Use default pageable if null
        if (pageable == null) {
            pageable = PageRequest.of(0, 20);
        }

        Page<User> users = userService.findAll(pageable);
        System.out.println("Page content: " + users.getContent());
        System.out.println("Found " + users.getTotalElements() + " users");

        if (users.hasContent()) {
            users.forEach(user -> System.out.println("User: " + user.getUsername() + ", Email: " + user.getPassword()));
        } else {
            System.out.println("No users found in the content part of the Page object.");
        }

        return users;
    }



}