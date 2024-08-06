package com.example.lm.Controller;

import com.example.lm.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.Map;

@Controller
public class UserViewController {

    @Autowired
    private UserService userService;

    @GetMapping("/banUser")
    public String banUserPage() {
        return "admin/banUser"; // 这里的"banUser"应该对应的是banUser.html
    }

    @PostMapping("/banUser")
    public ResponseEntity<?> banUser(@RequestBody Map<String, String> request) {
        try {
            int userId = Integer.parseInt(request.get("userId"));
            int banDuration = Integer.parseInt(request.get("banDuration"));
            System.out.println("userId: " + userId + ", banDuration: " + banDuration);
            userService.banUser(userId, banDuration);
            return ResponseEntity.ok(Collections.singletonMap("message", "User banned successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Error banning user"));
        }
    }

    @PostMapping("/removeBan")
    public ResponseEntity<?> removeBan(@RequestBody Map<String, String> request) {
        try {
            int userId = Integer.parseInt(request.get("userId"));
            userService.removeBan(userId);
            return ResponseEntity.ok(Collections.singletonMap("message", "Ban removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "Error removing ban"));
        }
    }

}
