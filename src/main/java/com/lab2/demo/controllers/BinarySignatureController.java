package com.lab2.demo.controllers;

import com.lab2.demo.dto.BinarySignaturePackage;
import com.lab2.demo.service.BinarySignaturePackageService;
import com.lab2.demo.binary.MultipartMixedResponseFactory;
import com.lab2.demo.dto.SignatureIdsRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/binary/signatures")
public class BinarySignatureController {

    private final BinarySignaturePackageService packageService;
    private final MultipartMixedResponseFactory responseFactory;

    public BinarySignatureController(BinarySignaturePackageService packageService,
                                     MultipartMixedResponseFactory responseFactory) {
        this.packageService = packageService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/full")
    public ResponseEntity<MultiValueMap<String, Object>> getFullPackage() {
        BinarySignaturePackage binaryPackage = packageService.buildFullPackage();

        return responseFactory.create(
                binaryPackage.getManifestBytes(),
                binaryPackage.getDataBytes()
        );
    }

    @GetMapping("/increment")
    public ResponseEntity<MultiValueMap<String, Object>> getIncrementPackage(
            @RequestParam("since")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant since) {

        BinarySignaturePackage binaryPackage = packageService.buildIncrementPackage(since);

        return responseFactory.create(
                binaryPackage.getManifestBytes(),
                binaryPackage.getDataBytes()
        );
    }

    @PostMapping("/by-ids")
    public ResponseEntity<MultiValueMap<String, Object>> getByIdsPackage(
            @Valid @RequestBody SignatureIdsRequest request) {

        BinarySignaturePackage binaryPackage = packageService.buildByIdsPackage(request.getIds());

        return responseFactory.create(
                binaryPackage.getManifestBytes(),
                binaryPackage.getDataBytes()
        );
    }
}