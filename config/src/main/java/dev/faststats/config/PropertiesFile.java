package dev.faststats.config;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Lenient reading and writing of properties files.
 */
final class PropertiesFile {
    private static final Charset UTF_32LE = Charset.forName("UTF-32LE");
    private static final Charset UTF_32BE = Charset.forName("UTF-32BE");
    private static final Comparator<String> KEY_ORDER = String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());
    private static final Pattern MALFORMED_UNICODE_ESCAPE = Pattern.compile("\\\\u(?![0-9a-fA-F]{4})");

    private PropertiesFile() {
    }

    /**
     * The contents of a properties file.
     *
     * @param properties      the properties
     * @param foreignEncoding whether the file is not plain UTF-8 and should be rewritten
     */
    record Contents(Properties properties, boolean foreignEncoding) {
    }

    /**
     * Reads the properties file, tolerating broken encodings and escapes.
     *
     * @return the contents, or {@code null} if the file does not exist
     * @throws IOException if the file exists but cannot be read
     */
    static @Nullable Contents read(final Path file) throws IOException {
        if (Files.notExists(file)) return null;
        if (!Files.isRegularFile(file)) throw new FileSystemException(file.toString(), null, "Not a regular file");

        final var decoded = decode(Files.readAllBytes(file));
        try {
            return new Contents(load(decoded.content()), decoded.foreignEncoding());
        } catch (final IllegalArgumentException e) {
            try {
                final var content = MALFORMED_UNICODE_ESCAPE.matcher(decoded.content()).replaceAll("\\\\\\\\u");
                return new Contents(load(content), decoded.foreignEncoding());
            } catch (final IllegalArgumentException again) {
                throw new IOException("Malformed config file", again);
            }
        }
    }

    /**
     * Writes the properties sorted alphabetically by key.
     */
    static void write(final Path file, final Properties properties, @Nullable final String comment) throws IOException {
        final var sorted = new Properties() {
            @Override
            public Set<Map.Entry<Object, Object>> entrySet() {
                final var entries = new TreeSet<Map.Entry<Object, Object>>(Comparator.comparing(
                        entry -> (String) entry.getKey(), KEY_ORDER));
                entries.addAll(super.entrySet());
                return entries;
            }
        };
        sorted.putAll(properties);

        final var parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (final var writer = Files.newBufferedWriter(file, UTF_8)) {
            sorted.store(writer, comment);
        }
    }

    /**
     * Finds the key actually used in the file, see {@link #normalize(String)}.
     * <p>
     * If the key is spelled in multiple ways, the exact spelling wins,
     * otherwise the alphabetically first one.
     *
     * @return the key as written in the file, or {@code null} if absent
     */
    static @Nullable String findKey(final Properties properties, final String key) {
        if (properties.containsKey(key)) return key;
        final var normalized = normalize(key);
        return properties.stringPropertyNames().stream()
                .filter(name -> normalize(name).equals(normalized))
                .min(KEY_ORDER)
                .orElse(null);
    }

    /**
     * Removes all spellings of the key that differ from the given one.
     */
    static void removeVariants(final Properties properties, final String key) {
        final var normalized = normalize(key);
        for (final var name : properties.stringPropertyNames())
            if (!name.equals(key) && normalize(name).equals(normalized)) properties.remove(name);
    }

    /**
     * Normalizes a key, ignoring case, whitespace, invisible characters, compatibility forms
     * like fullwidth letters, and the separators {@code -}, {@code _} and {@code .}.
     */
    static String normalize(final String key) {
        final var builder = new StringBuilder(key.length());
        Normalizer.normalize(key, Normalizer.Form.NFKC).codePoints()
                .filter(c -> !isBlank(c) && c != '-' && c != '_' && c != '.')
                .forEach(builder::appendCodePoint);
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    /**
     * Removes leading and trailing whitespace, including non-breaking spaces and invisible characters.
     */
    static String clean(final String value) {
        var start = 0;
        var end = value.length();
        while (start < end && isBlank(value.charAt(start))) start++;
        while (end > start && isBlank(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private static boolean isBlank(final int c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || Character.getType(c) == Character.FORMAT;
    }

    private static Properties load(final String content) throws IOException, IllegalArgumentException {
        final var properties = new Properties();
        properties.load(new StringReader(content));
        return properties;
    }

    private record Decoded(String content, boolean foreignEncoding) {
    }

    private static Decoded decode(final byte[] bytes) {
        final var charset = detectCharset(bytes);
        if (charset != null) {
            final var offset = bomLength(bytes, charset);
            return new Decoded(stripBom(new String(bytes, offset, bytes.length - offset, charset)), true);
        }
        try {
            final var content = UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return new Decoded(content, false);
        } catch (final CharacterCodingException e) {
            return new Decoded(stripBom(new String(bytes, ISO_8859_1)), true);
        }
    }

    private static String stripBom(final String content) {
        return content.startsWith("\uFEFF") ? content.substring(1) : content;
    }

    /**
     * Detects files that are not BOM-less UTF-8, using the byte order mark or,
     * for mostly ASCII content without one, the distribution of zero bytes.
     *
     * @return the charset, or {@code null} for BOM-less UTF-8 or unknown encodings
     */
    private static @Nullable Charset detectCharset(final byte[] bytes) {
        if (startsWith(bytes, 0xFF, 0xFE, 0x00, 0x00)) return UTF_32LE;
        if (startsWith(bytes, 0x00, 0x00, 0xFE, 0xFF)) return UTF_32BE;
        if (startsWith(bytes, 0xEF, 0xBB, 0xBF)) return UTF_8;
        if (startsWith(bytes, 0xFE, 0xFF)) return UTF_16BE;
        if (startsWith(bytes, 0xFF, 0xFE)) return UTF_16LE;

        final var zeros = new int[4];
        var anyZero = false;
        for (var i = 0; i < bytes.length; i++) {
            if (bytes[i] != 0) continue;
            zeros[i % 4]++;
            anyZero = true;
        }
        if (!anyZero) return null;

        final var quarter = bytes.length / 4 / 2;
        if (zeros[0] <= quarter && zeros[1] > quarter && zeros[2] > quarter && zeros[3] > quarter) return UTF_32LE;
        if (zeros[0] > quarter && zeros[1] > quarter && zeros[2] > quarter && zeros[3] <= quarter) return UTF_32BE;

        final var half = bytes.length / 2 / 2;
        final var even = zeros[0] + zeros[2];
        final var odd = zeros[1] + zeros[3];
        if (odd > half && even <= half / 2) return UTF_16LE;
        if (even > half && odd <= half / 2) return UTF_16BE;
        return null;
    }

    private static int bomLength(final byte[] bytes, final Charset charset) {
        if (charset == UTF_32LE) return startsWith(bytes, 0xFF, 0xFE, 0x00, 0x00) ? 4 : 0;
        if (charset == UTF_32BE) return startsWith(bytes, 0x00, 0x00, 0xFE, 0xFF) ? 4 : 0;
        if (charset == UTF_8) return startsWith(bytes, 0xEF, 0xBB, 0xBF) ? 3 : 0;
        if (charset == UTF_16BE) return startsWith(bytes, 0xFE, 0xFF) ? 2 : 0;
        return startsWith(bytes, 0xFF, 0xFE) ? 2 : 0;
    }

    private static boolean startsWith(final byte[] bytes, final int... prefix) {
        if (bytes.length < prefix.length) return false;
        for (var i = 0; i < prefix.length; i++) if ((bytes[i] & 0xFF) != prefix[i]) return false;
        return true;
    }
}
