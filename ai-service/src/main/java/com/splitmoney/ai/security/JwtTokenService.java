package com.splitmoney.ai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class JwtTokenService {

    @Value("${app.jwt.public-key-path}")
    private String publicKeyPath;

    @Value("${app.jwt.issuer}")
    private String expectedIssuer;

    private JwtParser parser;

    @PostConstruct
    public void init() throws Exception {
        RSAPublicKey publicKey = loadPublicKey(publicKeyPath);
        this.parser = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(expectedIssuer)
                .build();
    }

    public Claims parseToken(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    private RSAPublicKey loadPublicKey(String path) throws Exception {
        InputStream in = new ClassPathResource(path).getInputStream();
        String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decoded));
    }
}
