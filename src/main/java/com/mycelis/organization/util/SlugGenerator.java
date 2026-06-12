package com.mycelis.organization.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class SlugGenerator {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-+");

    /**
     * Convert "Acme Corp!" -> "acme-corp"
     */
    public String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String noWhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String stripped = NON_LATIN.matcher(normalized).replaceAll("");
        String singleDashed = MULTIPLE_DASHES.matcher(stripped).replaceAll("-");
        return singleDashed.toLowerCase()
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }
}