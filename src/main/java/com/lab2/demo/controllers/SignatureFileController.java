package com.lab2.demo.controllers;

import com.lab2.demo.dto.SignatureFilePresignedUrlResponse;
import com.lab2.demo.dto.SignatureFileUploadResponse;
import com.lab2.demo.dto.SignatureFileUrlsRequest;
import com.lab2.demo.model.AppUser;
import com.lab2.demo.service.AppUserService;
import com.lab2.demo.service.SignatureFileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/signature-files")
public class SignatureFileController {

    private final SignatureFileService signatureFileService;
    private final AppUserService appUserService;

    public SignatureFileController(SignatureFileService signatureFileService,
                                   AppUserService appUserService) {
        this.signatureFileService = signatureFileService;
        this.appUserService = appUserService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SignatureFileUploadResponse> uploadAndCreateFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "threatName", required = false) String threatName,
            Authentication authentication) {

        String uploadedBy = getCurrentUserName(authentication);

        SignatureFileUploadResponse response = signatureFileService.uploadAndCreateFromFile(
                file,
                threatName,
                uploadedBy
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/presigned-urls")
    public ResponseEntity<List<SignatureFilePresignedUrlResponse>> getPresignedUrls(
            @Valid @RequestBody SignatureFileUrlsRequest request) {

        List<SignatureFilePresignedUrlResponse> response =
                signatureFileService.getPresignedUrls(request);

        return ResponseEntity.ok(response);
    }

    private String getCurrentUserName(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();

        AppUser user = appUserService.getByUsernameOrFail(username);

        return user.getEmail();
    }
}