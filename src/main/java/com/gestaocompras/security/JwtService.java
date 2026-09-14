package com.gestaocompras.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(@Value("${jwt.secret}") String secretBase64,
            @Value("${jwt.expiration-ms}") long expiracaoMs) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET não está definido. Configure-o no ambiente de produção.");
        }
        this.chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expiracaoMs = expiracaoMs;
    }

    public String gerarToken(String email, String perfil, Integer versaoToken) {
        Date agora = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("perfil", perfil)
                .claim("vt", versaoToken == null ? 0 : versaoToken)
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + expiracaoMs))
                .signWith(chave)
                .compact();
    }

    public boolean tokenValido(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException excecao) {
            return false;
        }
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public int extrairVersaoToken(String token) {
        Object valor = extrairClaims(token).get("vt");
        return valor instanceof Number numero ? numero.intValue() : 0;
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
