package com.nyefan.clockenspiel.example;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

/**
 * @author nyefan
 */
public class TestFunctions {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFunctions.class);

    public static byte[] encrypt(String keyString, byte[] plainText) throws Exception {
//        AES_128/CBC/NoPadding
//        AES_128/CFB/NoPadding
//        AES_128/ECB/NoPadding
//        AES_128/GCM/NoPadding
//        AES_128/OFB/NoPadding
        Key    key    = new SecretKeySpec(Hex.decode(keyString), "AES");
        Cipher cipher = Cipher.getInstance("AES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plainText);
    }

    public static byte[] decrypt(String keyString, byte[] encryptedString) throws Exception {
        Key    key    = new SecretKeySpec(Hex.decode(keyString), "AES");
        Cipher cipher = Cipher.getInstance("AES", "BC");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encryptedString);
    }

    public static String signJWT(String payload, PrivateKey privateKey, SignatureAlgorithm signatureAlgorithm) {
        return Jwts.builder()
                .setPayload(payload)
                .signWith(signatureAlgorithm, privateKey)
                .compact();
    }

    public static Optional<Jwt> verifyToken(String token, PublicKey publicKey) {
        if (!Jwts.parser().isSigned(token)) {
            throw new UnsignedTokenException();
        }

        try {
            Jwt parsedToken = Jwts.parser().setSigningKey(publicKey).parse(token);
            return Optional.of(parsedToken);
        } catch (SignatureException se) {
            LOGGER.error(String.format("Token %s was not signed with the private key matching public key: %s", token, publicKey.toString()), se);
        }

        return Optional.empty();
    }

    public static class UnsignedTokenException extends IllegalArgumentException {
        private static final long serialVersionUID = 8873095048434114460L;
    }
}
