package com.nyefan.clockenspiel.example;

import com.nyefan.clockenspiel.Clock;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.EllipticCurveProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.nyefan.clockenspiel.example.TestFunctions.decrypt;
import static com.nyefan.clockenspiel.example.TestFunctions.encrypt;
import static com.nyefan.clockenspiel.example.TestFunctions.signJWT;
import static com.nyefan.clockenspiel.example.TestFunctions.verifyToken;

/**
 * @author nyefan
 */
public class PerformanceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTest.class);
    private static final Random RANDOM = new Random();

    public static void main(String... args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        Clock timer = new Clock.ClockBuilder().setThreadPoolSize(16).setIterationsPerCycle(100_000).build();
        perfTestEncrypt(timer);
        perfTestDecrypt(timer);
        timer = new Clock.ClockBuilder().setThreadPoolSize(16).setIterationsPerCycle(10_000).build();
        perfTestSignJWT(timer);
        perfTestVerifyJWT(timer);
    }

    private static void perfTestEncrypt(Clock timer) throws Exception {
        String testName = "perfTestEncrypt";
        Callable<Callable<byte[]>> generator = () -> {
            String payload = RANDOM.ints(65L).boxed().map(Integer::toHexString).collect(Collectors.joining());
            //128bit key
            byte[] keyBytes = new byte[16];
            RANDOM.nextBytes(keyBytes);

            final byte[] payloadBytes = payload.getBytes();
            final String keyString    = Hex.toHexString(keyBytes);

            return tryCatchMethod(() -> encrypt(keyString, payloadBytes), testName);
        };
        List<Duration> results = timer.time(generator, testName);
    }

    private static void perfTestDecrypt(Clock timer) throws Exception {
        String testName = "perfTestDecrypt";
        Callable<Callable<byte[]>> generator = () -> {
            String payload      = RANDOM.ints(65L).boxed().map(Integer::toHexString).collect(Collectors.joining());
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            //128bit key
            byte[] keyBytes = new byte[16];
            RANDOM.nextBytes(keyBytes);

            final String keyString        = Hex.toHexString(keyBytes);
            final byte[] encryptedPayload = encrypt(keyString, payloadBytes);

            return tryCatchMethod(() -> decrypt(keyString, encryptedPayload), testName);
        };
        List<Duration> results = timer.time(generator, testName);
    }

    private static void perfTestSignJWT(Clock timer) throws Exception {
        String testName = "perfTestSignJWT";
        Callable<Callable<String>> generator = () -> {

            final String             payload            = RANDOM.ints(65L).boxed().map(Integer::toHexString).collect(Collectors.joining());
            final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ES256;
            final PrivateKey         privateKey         = EllipticCurveProvider.generateKeyPair(signatureAlgorithm).getPrivate();

            return tryCatchMethod(() -> signJWT(payload, privateKey, signatureAlgorithm), testName);
        };
        List<Duration> results = timer.time(generator, testName);
    }

    private static void perfTestVerifyJWT(Clock timer) throws Exception {
        String testName = "perfTestVerifyJWT";
        Callable<Callable<Jwt>> generator = () -> {
            String             payload            = RANDOM.ints(65L).boxed().map(Integer::toHexString).collect(Collectors.joining());
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ES256;
            KeyPair            keyPair            = EllipticCurveProvider.generateKeyPair(signatureAlgorithm);

            final PublicKey publicKey = keyPair.getPublic();
            final String    token     = signJWT(payload, keyPair.getPrivate(), signatureAlgorithm);

            return tryCatchMethod(() -> verifyToken(token, publicKey), testName);
        };
        List<Duration> results = timer.time(generator, testName);
    }

    private static <T> Callable<T> tryCatchMethod(Callable<T> method, String testName) {
        return () -> {
            try {
                return method.call();
            } catch (Exception e) {
                return genericErrorMessage(testName, e);
            }
        };
    }

    private static <T> T genericErrorMessage(String testName, Throwable exception) {
        LOGGER.error(String.format("Error encountered in %s: ", testName), exception);
        //This is only ok because we're immediately throwing away the returned value
        return null;
    }
}
