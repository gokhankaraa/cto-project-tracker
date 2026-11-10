package com.kolaysoft.ctotracker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Sozlesme testleri icin on yuz kaynaklarini okur: elle yazilan TypeScript tipleri ve etiketleri backend ile ayni mi.
 * Testler backend klasorunde calisir (Maven); on yuz repository'de yan klasordur ({@code ../frontend}). CI'daki backend
 * isi tum repository'yi checkout ettigi icin dosyalar orada da vardir.
 *
 * <p>Ayristirma bilincli olarak basittir ve yalnizca bu projedeki yazim bicimini bekler: {@code export interface Ad {}}
 * icinde satir basina bir alan, {@code export const X = ['A', 'B'] as const} ve
 * {@code export const X: EnumLabels<..> = { DEGER: { label: '...' } }}. Bicim degisirse test acik bir hatayla kirilir.
 */
public final class FrontendSources {

    private static final Path SRC = Path.of("..", "frontend", "src");

    private FrontendSources() {
    }

    /** {@code frontend/src} altindaki dosyanin icerigi; yorumlar cikarilmis. */
    public static String read(String relativePath) {
        Path file = SRC.resolve(relativePath);
        try {
            return stripComments(Files.readString(file));
        } catch (IOException e) {
            throw new UncheckedIOException("On yuz dosyasi okunamadi: " + file.toAbsolutePath(), e);
        }
    }

    /** {@code frontend/src/api} altindaki (test olmayan) tum .ts dosyalari, birlestirilmis. */
    public static String readApiSources() {
        try (Stream<Path> files = Files.list(SRC.resolve("api"))) {
            StringBuilder all = new StringBuilder();
            files.filter(f -> f.toString().endsWith(".ts") && !f.toString().endsWith(".test.ts")).sorted()
                    .forEach(f -> all.append(read("api/" + f.getFileName())).append('\n'));
            return all.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** {@code export interface Ad} alan adlari; arayuz yoksa hata. */
    public static Set<String> interfaceFields(String source, String name) {
        Matcher body = Pattern.compile("export interface " + name + "(?:<[^>]*>)?\\s*\\{(.*?)\\n\\}", Pattern.DOTALL)
                .matcher(source);
        if (!body.find()) {
            throw new AssertionError("TypeScript arayuzu yok: " + name);
        }
        Set<String> fields = new LinkedHashSet<>();
        Matcher field = Pattern.compile("^\\s+(\\w+)\\??:", Pattern.MULTILINE).matcher(body.group(1));
        while (field.find()) {
            fields.add(field.group(1));
        }
        return fields;
    }

    /** {@code export const X = ['A', 'B'] as const} degerleri, sirasiyla. */
    public static List<String> constArray(String source, String name) {
        Matcher body = Pattern.compile("export const " + name + " = \\[(.*?)\\] as const", Pattern.DOTALL).matcher(source);
        if (!body.find()) {
            throw new AssertionError("TypeScript dizisi yok: " + name);
        }
        List<String> values = new ArrayList<>();
        Matcher value = Pattern.compile("'(\\w+)'").matcher(body.group(1));
        while (value.find()) {
            values.add(value.group(1));
        }
        return values;
    }

    /** {@code export const X: EnumLabels<..> = { DEGER: { label: '...' } }}: deger -> etiket. */
    public static Map<String, String> enumLabels(String source, String name) {
        Matcher body = Pattern.compile("export const " + name + ": EnumLabels<\\w+> = \\{(.*?)\\n\\}", Pattern.DOTALL)
                .matcher(source);
        if (!body.find()) {
            throw new AssertionError("Etiket tablosu yok: " + name);
        }
        Map<String, String> labels = new LinkedHashMap<>();
        Matcher entry = Pattern.compile("(\\w+): \\{ label: '([^']*)'").matcher(body.group(1));
        while (entry.find()) {
            labels.put(entry.group(1), entry.group(2));
        }
        return labels;
    }

    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)^\\s*//.*$", "");
    }
}
