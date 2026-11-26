package com.royaltyhouse.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

final class HashContrasena {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private HashContrasena() {
    }

    static String crear(String contrasena) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltHex = HEX.formatHex(salt);
        return saltHex + ":" + HEX.formatHex(digest(saltHex, contrasena));
    }

    static boolean coincide(String contrasena, String hashGuardado) {
        if (hashGuardado == null) {
            return false;
        }

        String[] partes = hashGuardado.split(":", 2);
        if (partes.length != 2) {
            return false;
        }

        try {
            return MessageDigest.isEqual(HEX.parseHex(partes[1]), digest(partes[0], contrasena));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] digest(String saltHex, String contrasena) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest((saltHex + ":" + contrasena).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM.", exception);
        }
    }
}
