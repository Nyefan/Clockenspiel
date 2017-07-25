package com.nyefan.clockenspiel.example;

import com.nyefan.clockenspiel.Clock;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.nyefan.clockenspiel.example.TestFunctions.encrypt;

/**
 * @author nyefan
 */
public class PerformanceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTest.class);
    private static final Random RANDOM = new Random();

    public static void main(String... args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        Clock timer = new Clock.ClockBuilder().setThreadPoolSize(8).setIterationsPerCycle(100_000).build();
        perfTestEncrypt(timer);
    }

    private static void perfTestEncrypt(Clock timer) throws Exception {
        String testName = "perfTestEncrypt";
        Callable<Callable<byte[]>> generator = () -> {
            String payload  = RANDOM.ints(65L).boxed().map(Integer::toHexString).collect(Collectors.joining());
            byte[] keyBytes = new byte[16];
            RANDOM.nextBytes(keyBytes);

            final byte[] payloadBytes = payload.getBytes();
            final String keyString    = Hex.toHexString(keyBytes);
            return () -> encrypt(keyString, payloadBytes);
        };
        List<Duration> results = timer.time(generator, testName);
    }
}
