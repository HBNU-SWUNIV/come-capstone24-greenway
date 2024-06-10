package com.example.demo.controller;

import com.example.demo.payload.request.*;
import com.example.demo.security.UserDetailsImpl;
import com.example.demo.service.EmailService;
import com.example.demo.service.JwtBlacklistService;
import com.example.demo.util.PasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import com.example.demo.payload.response.JwtResponse;
import com.example.demo.payload.response.MessageResponse;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtils;
import com.example.demo.model.User;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;

import javax.mail.MessagingException;
import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    JwtBlacklistService jwtBlacklistService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail()));
        } catch (AuthenticationException e) {
            // 로그인 실패 시 예외 처리
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Error: Invalid username or password."));
        }
    }

    @PostMapping(value = "/signup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        if (!signUpRequest.getPassword().equals(signUpRequest.getConfirmPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Passwords do not match."));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                encoder.encode(signUpRequest.getPassword()),
                signUpRequest.getName(),
                signUpRequest.getEmail());

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/find-username")
    public ResponseEntity<?> findUsername(@Valid @RequestBody FindUsernameRequest request) {
        String email = request.getEmail();
        String name = request.getName();

        User user = userRepository.findByEmailAndName(email, name);

        if (user != null) {
            return ResponseEntity.ok(new MessageResponse("Your username is: " + user.getUsername()));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("User with this email and name does not exist."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) throws MessagingException {
        String username = request.getUsername();
        String name = request.getName();

        Optional<User> userOptional = userRepository.findByUsernameAndName(username, name);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // 등록된 이메일 반환
            String email = user.getEmail();

            // 임시 비밀번호 생성
            String temporaryPassword = PasswordGenerator.generateRandomPassword();

            // 임시 비밀번호로 사용자 비밀번호 업데이트
            user.setPassword(passwordEncoder.encode(temporaryPassword));
            userRepository.save(user);

            // 이메일로 임시 비밀번호 전송
            String subject = "Temporary Password for Account Recovery";
            String message = "Your temporary password is: " + temporaryPassword;
            emailService.sendEmail(email, subject, message);

            return ResponseEntity.ok(new MessageResponse("Temporary password sent to your email address: " + email));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("User with this username and name does not exist."));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = request.getUsername();
        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return ResponseEntity.ok(new MessageResponse("Password changed successfully!"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Current password is incorrect."));
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("User not found."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@Valid @RequestBody TokenRequest tokenRequest) {
        String token = tokenRequest.getToken();
        long expirationTime = jwtUtils.getExpirationFromToken(token);

        jwtBlacklistService.blacklistToken(token, expirationTime);

        return ResponseEntity.ok(new MessageResponse("User logged out successfully."));
    }

    @PostMapping("/check-token")
    public ResponseEntity<?> checkToken(@Valid @RequestBody TokenRequest tokenRequest) {
        String token = tokenRequest.getToken();
        boolean isBlacklisted = jwtBlacklistService.isTokenBlacklisted(token);

        if (isBlacklisted) {
            return ResponseEntity.ok(new MessageResponse("Token is blacklisted."));
        } else {
            return ResponseEntity.ok(new MessageResponse("Token is not blacklisted."));
        }
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(@Valid @RequestBody DeleteAccountRequest deleteAccountRequest) {
        String username = deleteAccountRequest.getUsername();
        String password = deleteAccountRequest.getPassword();

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (passwordEncoder.matches(password, user.getPassword())) {
                userRepository.delete(user);
                return ResponseEntity.ok(new MessageResponse("User account deleted successfully!"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Current password is incorrect."));
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("User not found."));
        }
    }
}