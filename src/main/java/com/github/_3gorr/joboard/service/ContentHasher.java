package com.github._3gorr.joboard.service;

import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.source.RawVacancy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ContentHasher {

    private ContentHasher() {
    }

    public static String hash(RawVacancy v) {
        Salary s = v.salary();
        String payload = String.join("",
                nullToEmpty(v.title()),
                nullToEmpty(v.company()),
                nullToEmpty(v.city()),
                s.from() == null ? "" : s.from().toString(),
                s.to() == null ? "" : s.to().toString(),
                nullToEmpty(s.currency()),
                nullToEmpty(v.employmentType()),
                nullToEmpty(v.description()),
                nullToEmpty(v.requirements()));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
