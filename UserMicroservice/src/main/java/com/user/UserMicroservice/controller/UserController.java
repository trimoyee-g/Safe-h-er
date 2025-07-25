package com.user.UserMicroservice.controller;

import com.user.UserMicroservice.entity.User;
import com.user.UserMicroservice.service.UserService;
import com.user.UserMicroservice.util.AccessControlUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User user1 = userService.saveUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user1);
    }

    @GetMapping("/{userId}")
    @CircuitBreaker(name="ratingPlaceBreaker", fallbackMethod = "ratingPlaceFallback")
    @Retry(name = "ratingPlaceRetry", fallbackMethod = "ratingPlaceFallback")
    @RateLimiter(name = "ratingPlaceRateLimiter", fallbackMethod = "ratingPlaceFallback")
    public ResponseEntity<User> getSingleUser(@PathVariable String userId) {
        User user1 = userService.getUser(userId);
        return ResponseEntity.ok(user1);
    }

    @GetMapping
    @CircuitBreaker(name="ratingPlaceBreaker", fallbackMethod = "ratingPlaceFallback")
    @Retry(name = "ratingPlaceRetry", fallbackMethod = "ratingPlaceFallback")
    @RateLimiter(name = "ratingPlaceRateLimiter", fallbackMethod = "ratingPlaceFallback")
    public ResponseEntity<List<User>> getAllUser() {
        List<User> allUser = userService.getAllUser();
        return ResponseEntity.ok(allUser);
    }

    @GetMapping("/search")
    public ResponseEntity<?> getUsersByName(@RequestParam("name") String name) {
        List<User> users = userService.getUsersByName(name);
        if (users.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("No users found");
        }
        return ResponseEntity.ok(users);
    }


    public ResponseEntity<User> ratingPlaceFallback(String userId, Exception ex){
        User user = User.builder()
                .userName("dummy user")
                .userEmail("dummy@email.com")
                .userAbout("too many requests or some service is down")
                .userGender("NA")
                .userId("000")
                .build();
        return new ResponseEntity<>(user, HttpStatus.TOO_MANY_REQUESTS);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable String userId, @RequestBody User user, @RequestHeader("X-User-Id") String requesterId, @RequestHeader("X-User-Role") String role) {
        if (!AccessControlUtil.isSelf(requesterId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        User updatedUser = userService.updateUser(userId, user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId, @RequestHeader("X-User-Id") String requesterId, @RequestHeader("X-User-Role") String role) {
        if (!AccessControlUtil.isAdminOrSelf(role, requesterId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }
        userService.deleteUser(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("User deleted successfully");
    }
}
