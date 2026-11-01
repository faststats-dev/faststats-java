package dev.faststats.config;

import dev.faststats.internal.PlatformLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MisconfiguredConfigTest {
    private final PlatformLoggerFactory loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
    });

    @TempDir
    private Path directory;
    private Path file;

    @BeforeEach
    public void setUp() {
        file = directory.resolve("faststats").resolve("config.properties");
    }

    private CustomConfig read() {
        return assertDoesNotThrow(() -> CustomConfig.builder(file, loggerFactory).build());
    }

    private void store(final String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, UTF_8);
    }

    private Properties load() throws IOException {
        try (final var reader = Files.newBufferedReader(file, UTF_8)) {
            final var properties = new Properties();
            properties.load(reader);
            return properties;
        }
    }

    private static void assertDisabled(final CustomConfig config) {
        assertFalse(config.enabled(), "enabled");
        assertFalse(config.submitMetrics(), "submitMetrics");
        assertFalse(config.additionalMetrics(), "additionalMetrics");
        assertFalse(config.errorTracking(), "errorTracking");
    }

    @Test
    public void onlyEnabledFalse() throws IOException {
        store("enabled=false\n");

        assertDisabled(read());
        final var properties = load();
        assertEquals("false", properties.getProperty("enabled"));
        assertEquals("3", properties.getProperty("configVersion"));
        assertDisabled(read());
    }

    @Test
    public void onlyEnabledFalseWithNewerConfigVersion() throws IOException {
        store("enabled=false\nconfigVersion=999\n");

        assertDisabled(read());
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "FALSE", "False", " false ", "no", "off", "0", "disabled", "DISABLED", "", "null"})
    public void enabledDisabledVariants(final String value) throws IOException {
        store("enabled=" + value + "\n");

        assertDisabled(read());
    }

    @ParameterizedTest
    @ValueSource(strings = {"enabled: false", "enabled:false", "enabled : false", "enabled :false", "Enabled : false", "  enabled  :  false  ", "enabled false", "enabled  =  false", "  enabled=false"})
    public void alternativeSeparators(final String line) throws IOException {
        store(line + "\n");

        assertDisabled(read());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISABLED", "", "null", "none", "0", "-", "00000000-0000-0000-0000", "\\u0000"})
    public void invalidServerId(final String value) throws IOException {
        store("serverId=" + value + "\nenabled=false\n");

        final var config = read();
        assertDisabled(config);
        assertEquals(config.serverId().toString(), load().getProperty("serverId"));
    }

    @Test
    public void serverIdUnderWrongKey() throws IOException {
        store("server-id=DISABLED\nenabled=false\n");

        assertDisabled(read());
    }

    @Test
    public void shortFormServerIdIsNormalized() throws IOException {
        store("serverId=1-2-3-4-5\n");

        final var config = read();
        assertEquals(config.serverId().toString(), load().getProperty("serverId"));
    }

    @Test
    public void emptyFile() throws IOException {
        store("");

        final var config = read();
        assertTrue(config.firstRun());
        assertEquals(config.serverId().toString(), load().getProperty("serverId"));
    }

    @Test
    public void onlyComments() throws IOException {
        store("# enabled=false\n! comment\n\n   \n");

        assertTrue(read().firstRun());
    }

    @Test
    public void duplicateKeysLastOneWins() throws IOException {
        store("enabled=true\nenabled=false\n");

        assertDisabled(read());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "", "-5", "99999999999", "3.0", "three"})
    public void invalidConfigVersion(final String value) throws IOException {
        store("enabled=false\nconfigVersion=" + value + "\n");

        assertDisabled(read());
    }

    @Test
    public void allValuesEmpty() throws IOException {
        store("""
                enabled=
                submitMetrics=
                submitAdditionalMetrics=
                submitErrors=
                serverId=
                debug=
                configVersion=
                """);

        final var config = read();
        assertDisabled(config);
        assertFalse(config.debug());
    }

    @Test
    public void malformedUnicodeEscape() throws IOException {
        store("enabled=false\nserverId=\\uZZZZ\n");

        assertDisabled(read());
    }

    @Test
    public void windowsPathBackslashes() throws IOException {
        store("enabled=false\nsomething=C:\\users\\name\n");

        assertDisabled(read());
    }

    @Test
    public void latin1EncodedFile() throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "# Datenschutz: Ä Ö Ü ß\nenabled=false\n", ISO_8859_1);

        assertDisabled(read());
    }

    @Test
    public void utf8ByteOrderMark() throws IOException {
        store("\uFEFFenabled=false\n");

        assertDisabled(read());
    }

    @Test
    public void binaryGarbage() throws IOException {
        Files.createDirectories(file.getParent());
        final var bytes = new byte[1024];
        new Random(42).nextBytes(bytes);
        Files.write(file, bytes);

        final var config = read();
        assertTrue(config.firstRun());
        assertEquals(config.serverId().toString(), load().getProperty("serverId"));
    }

    @Test
    public void readOnlyFileWithMissingKeys() throws IOException {
        store("enabled=false\n");
        assertTrue(file.toFile().setReadOnly());

        assertDisabled(read());
        assertEquals("enabled=false\n", Files.readString(file, UTF_8));
    }

    @Test
    public void readOnlyFilePersist() throws IOException {
        store("enabled=false\n");
        final var config = read();
        assertTrue(file.toFile().setReadOnly());

        assertDoesNotThrow(config::persist);
    }

    @Test
    public void configPathIsDirectory() throws IOException {
        Files.createDirectories(file);

        final var config = read();
        assertDisabled(config);
        assertFalse(config.firstRun());
        assertTrue(Files.isDirectory(file));
        assertDoesNotThrow(config::persist);
    }

    @Test
    public void unreadableFileDisablesEvenWithEnabledOverride() throws IOException {
        Files.createDirectories(file);

        assertDisabled(assertDoesNotThrow(() -> CustomConfig.builder(file, loggerFactory).enabled(true).build()));
    }

    @Test
    public void unknownKeysSurviveRewrite() throws IOException {
        store("enabled=false\nmy-note=do not delete\n");

        read();

        assertEquals("do not delete", load().getProperty("my-note"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Enabled=false", "ENABLED=false", "Enabled = false", "  Enabled  =  false  ", "eNaBlEd:false", "\tEnabled\t=\tfalse\t"})
    public void keysAreCaseInsensitive(final String line) throws IOException {
        store(line + "\n");

        assertDisabled(read());
        final var properties = load();
        assertEquals("false", properties.getProperty("enabled"));
        assertEquals(1, properties.stringPropertyNames().stream().filter("enabled"::equalsIgnoreCase).count());
        assertDisabled(read());
    }

    @Test
    public void wrongCaseCustomKey() throws IOException {
        final var field = CustomConfig.Field.bool("onboarding-screen-seen", false);
        store("enabled=false\nOnboarding-Screen-Seen = true\n");

        assertTrue(CustomConfig.builder(file, loggerFactory).field(field).build().get(field));
        assertEquals("true", load().getProperty("onboarding-screen-seen"));
        assertFalse(load().containsKey("Onboarding-Screen-Seen"));
    }

    @Test
    public void exactSpellingWins() throws IOException {
        store("enabled=false\nENABLED=true\n");

        assertDisabled(read());
        assertSingleSpelling("enabled", "false");
    }

    @Test
    public void exactSpellingWinsRegardlessOfPosition() throws IOException {
        store("Enabled=true\nENABLED=true\nenabled=false\n");

        assertDisabled(read());
        assertSingleSpelling("enabled", "false");
    }

    @Test
    public void alphabeticallyFirstSpellingWinsWithoutExactKey() throws IOException {
        // "ENABLED" sorts before "Enabled", regardless of the order in the file
        store("Enabled=true\nENABLED=false\n");
        assertDisabled(read());
        assertSingleSpelling("enabled", "false");

        store("ENABLED=false\nEnabled=true\n");
        assertDisabled(read());
        assertSingleSpelling("enabled", "false");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "submit_metrics", "submit-metrics", "submit.metrics", "SUBMIT_METRICS", "Submit-Metrics",
            "submit\\ metrics", "submitmetrics", "_submitMetrics_",
            "submit\u00A0metrics", "submitMetrics\u00A0", "\u00A0submitMetrics", "submit\u200Bmetrics",
            "\u200BsubmitMetrics", "submitMetrics\u2060", "\uFF53\uFF55\uFF42\uFF4D\uFF49\uFF54metrics"
    })
    public void similarKeysAreMerged(final String key) throws IOException {
        store(key + "=false\n");

        final var config = read();
        assertTrue(config.enabled());
        assertFalse(config.submitMetrics(), key);
        assertSingleSpelling("submitMetrics", "false");

        store("submitMetrics=true\n" + key + "=false\n");

        assertTrue(read().submitMetrics(), key);
        assertSingleSpelling("submitMetrics", "true");
    }

    @ParameterizedTest
    @ValueSource(strings = {"false\u00A0", "\u00A0false", "false\u200B", "\u200Bfalse", "\uFEFFfalse", "false\u3000"})
    public void invisibleCharactersInValues(final String value) throws IOException {
        store("enabled=" + value + "\nsubmitErrors=true" + value.replace("false", "") + "\n");

        final var config = read();
        assertDisabled(config);
        assertEquals("false", load().getProperty("enabled"));
    }

    @Test
    public void invisibleCharactersAroundTrueKeepTrue() throws IOException {
        store("submitErrors=\u00A0true\u200B\n");

        assertTrue(read().errorTracking());
        assertEquals("true", load().getProperty("submitErrors"));
    }

    @Test
    public void invisibleCharactersAroundServerIdKeepServerId() throws IOException {
        final var serverId = UUID.randomUUID();
        store("serverId=\u00A0" + serverId + "\u200B\n");

        assertEquals(serverId, read().serverId());
        assertEquals(serverId.toString(), load().getProperty("serverId"));
    }

    @Test
    public void everySpellingAtOnce() throws IOException {
        store("""
                Enabled=true
                ENABLED = true
                en_abled=true
                enabled=false
                \u00A0enabled\u00A0=true
                e-n-a-b-l-e-d: true
                serverId=DISABLED
                server_id=DISABLED
                SERVER-ID=DISABLED
                submit_metrics=false
                submitMetrics=true
                """);

        final var config = read();
        assertDisabled(config);
        assertSingleSpelling("enabled", "false");
        assertSingleSpelling("serverId", config.serverId().toString());
        assertSingleSpelling("submitMetrics", "true");

        config.persist();
        assertSingleSpelling("enabled", "false");
        assertSingleSpelling("serverId", config.serverId().toString());
        // persist writes the effective in-memory state, which is disabled
        assertSingleSpelling("submitMetrics", "false");
        assertDisabled(read());
    }

    @Test
    public void variantsAreRemovedOnPersistWithoutRewriteOnLoad() throws IOException {
        // a newer config version prevents the rewrite on load, so the variants survive until persist
        store("ENABLED=false\nconfigVersion=99\n");
        final var config = read();
        assertDisabled(config);
        assertTrue(load().containsKey("ENABLED"));

        config.persist();

        assertSingleSpelling("enabled", "false");
    }

    @Test
    public void throwingSerializerKeepsOtherSpelling() throws IOException {
        store("enabled=false\nBROKEN=keep me\n");

        CustomConfig.builder(file, loggerFactory).field(BROKEN_SERIALIZER).build().persist();

        assertEquals("keep me", load().getProperty("BROKEN"));
    }

    private void assertSingleSpelling(final String key, final String value) throws IOException {
        final var properties = load();
        final var spellings = properties.stringPropertyNames().stream()
                .filter(name -> PropertiesFile.normalize(name).equals(PropertiesFile.normalize(key)))
                .toList();
        assertEquals(List.of(key), spellings, "spellings of " + key);
        assertEquals(value, properties.getProperty(key));
    }

    @Test
    public void wrongCaseKeysArePersistedCanonically() throws IOException {
        store("Enabled=false\nSUBMITMETRICS=false\n");
        final var config = read();

        config.persist();

        final var properties = load();
        assertEquals("false", properties.getProperty("enabled"));
        assertEquals("false", properties.getProperty("submitMetrics"));
        assertFalse(properties.containsKey("Enabled"));
        assertFalse(properties.containsKey("SUBMITMETRICS"));
    }

    @Test
    public void typoInKeyIsKeptAndTreatedAsFirstRun() throws IOException {
        store("enable=false\n");

        // nothing we know of, so the user gets the onboarding message and a chance to opt out before anything is sent
        assertTrue(read().firstRun());
        assertEquals("false", load().getProperty("enable"));
    }

    @Test
    public void keyFromOtherConfigFormats() throws IOException {
        store("enabled = \"false\"\n");

        assertDisabled(read());
    }

    @Test
    public void yamlStyle() throws IOException {
        store("enabled: false\nserverId: " + UUID.randomUUID() + "\n");

        assertDisabled(read());
    }

    private static final CustomConfig.Field<String> BROKEN_SERIALIZER = new CustomConfig.Field<>("broken", () -> "value",
            value -> value, value -> {
        throw new IllegalStateException("serializer bug");
    });

    @Test
    public void throwingSerializerDoesNotPreventSaving() throws IOException {
        store("enabled=false\nbroken=keep me\n");

        final var config = assertDoesNotThrow(() -> CustomConfig.builder(file, loggerFactory).field(BROKEN_SERIALIZER).build());

        assertDisabled(config);
        final var properties = load();
        assertEquals("false", properties.getProperty("enabled"));
        assertEquals("3", properties.getProperty("configVersion"));
        assertEquals("keep me", properties.getProperty("broken"));
    }

    @Test
    public void throwingSerializerDoesNotPreventPersisting() throws IOException {
        final var config = CustomConfig.builder(file, loggerFactory).field(BROKEN_SERIALIZER).build();
        config.submitMetrics(false);

        assertDoesNotThrow(config::persist);
        assertEquals("false", load().getProperty("submitMetrics"));
    }

    @Test
    public void nullFromSerializerDoesNotPreventSaving() throws IOException {
        final var field = new CustomConfig.Field<String>("null", () -> "value", value -> value, value -> null);

        final var config = assertDoesNotThrow(() -> CustomConfig.builder(file, loggerFactory).field(field).build());

        assertDoesNotThrow(config::persist);
        assertEquals(config.serverId().toString(), load().getProperty("serverId"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTF-16", "UTF-16LE", "UTF-16BE", "UTF-32", "UTF-32LE", "UTF-32BE", "x-UTF-16LE-BOM", "X-UTF-32LE-BOM", "UTF-8-BOM"})
    public void otherUnicodeEncodings(final String encoding) throws IOException {
        final var content = "# Datenschutz: \u00E4\u00F6\u00FC \u2014 \uD83D\uDE00\nenabled=false\nmy-note=\u00E4\u00F6\u00FC\n";
        final var bytes = encoding.equals("UTF-8-BOM")
                ? ("\uFEFF" + content).getBytes(UTF_8)
                : content.getBytes(Charset.forName(encoding));
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);

        final var config = read();
        assertDisabled(config);
        assertFalse(config.firstRun());
        assertRewrittenAsUtf8();
        assertEquals("\u00E4\u00F6\u00FC", load().getProperty("my-note"));
        assertDisabled(read());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTF-16LE", "UTF-16BE", "UTF-32LE", "UTF-32BE"})
    public void otherUnicodeEncodingsWithoutComments(final String encoding) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, "enabled=false\n".getBytes(Charset.forName(encoding)));

        assertDisabled(read());
        assertRewrittenAsUtf8();
    }

    @Test
    public void legacyWindowsEncodingIsRewritten() throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, "# \u201Cquotes\u201D \u20AC\nenabled=false\n".getBytes(Charset.forName("windows-1252")));

        assertDisabled(read());
        assertRewrittenAsUtf8();
    }

    @Test
    public void truncatedUtf16() throws IOException {
        final var bytes = "\uFEFFenabled=false\n".getBytes(UTF_16LE);
        Files.createDirectories(file.getParent());
        Files.write(file, Arrays.copyOf(bytes, bytes.length - 1));

        assertDisabled(read());
    }

    @Test
    public void foreignEncodingIsKeptWhenRewriteIsBlocked() throws IOException {
        final var bytes = "enabled=false\nconfigVersion=99\n".getBytes(UTF_16LE);
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);

        assertDisabled(read());
        assertArrayEquals(bytes, Files.readAllBytes(file));
    }

    @Test
    public void plainUtf8IsNotRewritten() throws IOException {
        final var content = "# \u00E4\u00F6\u00FC\nenabled=false\nsubmitMetrics=true\nsubmitAdditionalMetrics=true\n"
                + "submitErrors=true\nserverId=" + UUID.randomUUID() + "\ndebug=false\nconfigVersion=3\n";
        store(content);

        read();

        assertEquals(content, Files.readString(file, UTF_8));
    }

    private void assertRewrittenAsUtf8() throws IOException {
        final var bytes = Files.readAllBytes(file);
        assertFalse(bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF, "BOM");
        for (final var b : bytes) assertNotEquals(0, b, "zero byte");
        assertDoesNotThrow(() -> UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)));
    }
}
