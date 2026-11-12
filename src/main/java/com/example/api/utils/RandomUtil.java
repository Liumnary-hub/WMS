package com.example.api.utils;

import java.security.SecureRandom;

public final class RandomUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String next() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

}
