package com.lab2.demo.service;

import com.lab2.demo.signature.JsonCanonicalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SigningService {

    private final JsonCanonicalizer jsonCanonicalizer;
    private final SignatureKeyStoreService keyStoreService;

    public String sign(Object payload) {
        try {
            String canonicalJson = jsonCanonicalizer.canonizeJson(payload);
            byte[] canonicalBytes = canonicalJson.getBytes(StandardCharsets.UTF_8);

            PrivateKey privateKey = keyStoreService.getPrivateKey();

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(canonicalBytes);

            byte[] signatureBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign payload", ex);
        }
    }
}