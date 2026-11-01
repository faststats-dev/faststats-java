package dev.faststats.config;

import dev.faststats.internal.PlatformLoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CustomConfigTest {
    private static final CustomConfig.Field<Boolean> SCREEN_SEEN = CustomConfig.Field.bool("onboarding-screen-seen", false);
    private static final CustomConfig.Field<Integer> COUNTER = CustomConfig.Field.integer("counter", 5);

    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final PlatformLoggerFactory loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> messages.add(message));

    @TempDir
    private Path directory;
    private Path file;

    @BeforeEach
    public void setUp() {
        loggerFactory.setDebug(true);
        file = directory.resolve("faststats").resolve("config.properties");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("faststats.enabled");
        System.clearProperty("faststats.debug");
    }

    private CustomConfig.Builder builder() {
        return CustomConfig.builder(file, loggerFactory);
    }

    private Properties load() throws IOException {
        try (final var reader = Files.newBufferedReader(file, UTF_8)) {
            final var properties = new Properties();
            properties.load(reader);
            return properties;
        }
    }

    private void store(final String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, UTF_8);
    }

    private static String validConfig(final String... extraLines) {
        return String.join("\n", List.of(
                "enabled=true",
                "submitMetrics=true",
                "submitAdditionalMetrics=true",
                "submitErrors=true",
                "serverId=" + UUID.randomUUID(),
                "debug=false",
                "configVersion=3"
        )) + "\n" + String.join("\n", extraLines);
    }

    @Test
    public void firstRunCreatesFileWithDefaults() throws IOException {
        final var config = builder().build();

        assertTrue(config.firstRun());
        assertTrue(Files.isRegularFile(file));
        assertTrue(config.enabled());
        assertTrue(config.submitMetrics());
        assertTrue(config.additionalMetrics());
        assertTrue(config.errorTracking());
        assertFalse(config.debug());

        final var properties = load();
        assertEquals("true", properties.getProperty("enabled"));
        assertEquals("true", properties.getProperty("submitMetrics"));
        assertEquals("true", properties.getProperty("submitAdditionalMetrics"));
        assertEquals("true", properties.getProperty("submitErrors"));
        assertEquals("false", properties.getProperty("debug"));
        assertEquals("3", properties.getProperty("configVersion"));
        assertEquals(config.serverId().toString(), properties.getProperty("serverId"));
    }

    @Test
    public void secondReadKeepsServerIdAndFile() throws IOException {
        final var first = builder().build();
        final var content = Files.readString(file, UTF_8);

        final var second = builder().build();

        assertFalse(second.firstRun());
        assertEquals(first.serverId(), second.serverId());
        assertEquals(content, Files.readString(file, UTF_8));
    }

    @Test
    public void defaultCommentIsWritten() throws IOException {
        builder().build();
        assertTrue(Files.readString(file, UTF_8).contains("https://faststats.dev/abuse"));
    }

    @Test
    public void customCommentIsWritten() throws IOException {
        builder().comment("My custom header\nSecond line").build();

        final var lines = Files.readAllLines(file, UTF_8);
        assertEquals("#My custom header", lines.get(0));
        assertEquals("#Second line", lines.get(1));
        assertFalse(String.join("\n", lines).contains("https://faststats.dev/abuse"));
    }

    @Test
    public void nullCommentOmitsHeader() throws IOException {
        builder().comment(null).build();
        final var comments = Files.readAllLines(file, UTF_8).stream().filter(line -> line.startsWith("#")).toList();
        assertEquals(1, comments.size());
    }

    @Test
    public void customFieldDefaultIsWritten() throws IOException {
        final var config = builder().field(SCREEN_SEEN).field(COUNTER).build();

        assertFalse(config.get(SCREEN_SEEN));
        assertEquals(5, config.get(COUNTER));
        assertEquals("false", load().getProperty("onboarding-screen-seen"));
        assertEquals("5", load().getProperty("counter"));
    }

    @Test
    public void customFieldIsReadFromFile() throws IOException {
        store(validConfig("onboarding-screen-seen=true", "counter=42"));

        final var config = builder().field(SCREEN_SEEN).field(COUNTER).build();

        assertTrue(config.get(SCREEN_SEEN));
        assertEquals(42, config.get(COUNTER));
    }

    @Test
    public void customFieldPersists() throws IOException {
        final var config = builder().field(SCREEN_SEEN).build();
        config.set(SCREEN_SEEN, true);
        config.persist();

        assertEquals("true", load().getProperty("onboarding-screen-seen"));
        assertTrue(builder().field(SCREEN_SEEN).build().get(SCREEN_SEEN));
    }

    @Test
    public void missingCustomFieldIsAddedToExistingFile() throws IOException {
        store(validConfig());

        builder().field(SCREEN_SEEN).build();

        assertEquals("false", load().getProperty("onboarding-screen-seen"));
        assertTrue(messages.stream().anyMatch(message -> message.contains("Missing FastStats config property 'onboarding-screen-seen'")));
    }

    @Test
    public void customFieldWithCustomType() throws IOException {
        final var field = new CustomConfig.Field<>("tags", List::<String>of,
                value -> List.of(value.split(",")), value -> String.join(",", value));
        store(validConfig("tags=a,b,c"));

        final var config = builder().field(field).build();

        assertEquals(List.of("a", "b", "c"), config.get(field));
    }

    @Test
    public void duplicateFieldIsRejected() {
        final var builder = builder().field(SCREEN_SEEN);
        assertThrows(IllegalArgumentException.class, () -> builder.field(CustomConfig.Field.bool("onboarding-screen-seen", true)));
        assertThrows(IllegalArgumentException.class, () -> builder.field(CustomConfig.Field.bool("enabled", true)));
        assertThrows(IllegalArgumentException.class, () -> builder.field(CustomConfig.Field.integer("configVersion", 1)));
        assertThrows(IllegalArgumentException.class, () -> builder.field(CustomConfig.Field.bool("Onboarding-Screen-Seen", true)));
        assertThrows(IllegalArgumentException.class, () -> builder.field(CustomConfig.Field.bool("ENABLED", true)));
        assertThrows(IllegalArgumentException.class, () -> builder.field(CustomConfig.Field.bool("onboarding_screen_seen", true)));
        assertThrows(IllegalArgumentException.class, () -> builder.field(CustomConfig.Field.bool("submit-metrics", true)));
    }

    @Test
    public void unregisteredFieldIsRejected() {
        final var config = builder().build();

        assertFalse(config.has(SCREEN_SEEN));
        assertThrows(IllegalArgumentException.class, () -> config.get(SCREEN_SEEN));
        assertThrows(IllegalArgumentException.class, () -> config.set(SCREEN_SEEN, true));
    }

    @Test
    public void fieldWithSameKeyButDifferentInstanceIsNotRegistered() {
        final var config = builder().field(SCREEN_SEEN).build();

        final var other = CustomConfig.Field.bool("onboarding-screen-seen", false);
        assertTrue(config.has(SCREEN_SEEN));
        assertFalse(config.has(other));
    }

    @Test
    public void defaultFieldsAreRegistered() {
        final var config = builder().build();

        assertTrue(config.has(CustomConfig.ENABLED));
        assertTrue(config.has(CustomConfig.SUBMIT_METRICS));
        assertTrue(config.has(CustomConfig.SUBMIT_ADDITIONAL_METRICS));
        assertTrue(config.has(CustomConfig.SUBMIT_ERRORS));
        assertTrue(config.has(CustomConfig.SERVER_ID));
        assertTrue(config.has(CustomConfig.DEBUG));
    }

    @Test
    public void invalidValueFallsBackToDefaultAndIsRewritten() throws IOException {
        store(validConfig("counter=not-a-number"));

        final var config = builder().field(COUNTER).build();

        assertEquals(5, config.get(COUNTER));
        assertEquals("5", load().getProperty("counter"));
    }

    @Test
    public void invalidServerIdIsRegenerated() throws IOException {
        store(validConfig().replaceFirst("serverId=.*", "serverId=invalid"));

        final var config = builder().build();

        assertEquals(config.serverId().toString(), load().getProperty("serverId"));
    }

    @Test
    public void tooLongServerIdIsTruncated() throws IOException {
        final var serverId = UUID.randomUUID();
        store(validConfig().replaceFirst("serverId=.*", "serverId=" + serverId + "garbage"));

        final var config = builder().build();

        assertEquals(serverId, config.serverId());
        assertEquals(serverId.toString(), load().getProperty("serverId"));
    }

    @Test
    public void nonCanonicalValueIsRewritten() throws IOException {
        store(validConfig().replace("enabled=true", "enabled=TRUE"));

        builder().build();

        assertEquals("true", load().getProperty("enabled"));
    }

    @Test
    public void valuesAreTrimmed() throws IOException {
        store(validConfig("counter=  7  "));

        assertEquals(7, builder().field(COUNTER).build().get(COUNTER));
    }

    @Test
    public void olderConfigVersionIsUpgraded() throws IOException {
        store(validConfig().replace("configVersion=3", "configVersion=2"));

        builder().build();

        assertEquals("3", load().getProperty("configVersion"));
        assertTrue(messages.stream().anyMatch(message -> message.contains("Updating config version from 2 to 3")));
    }

    @Test
    public void missingConfigVersionIsAdded() throws IOException {
        store(validConfig().replace("configVersion=3", ""));

        builder().build();

        assertEquals("3", load().getProperty("configVersion"));
    }

    @Test
    public void newerConfigVersionIsNotOverwritten() throws IOException {
        final var content = validConfig("counter=invalid").replace("configVersion=3", "configVersion=99");
        store(content);

        final var config = builder().field(COUNTER).build();

        assertEquals(5, config.get(COUNTER));
        assertEquals(content, Files.readString(file, UTF_8));
    }

    @Test
    public void disabledConfigDisablesAllFeatures() throws IOException {
        store(validConfig().replace("enabled=true", "enabled=false"));

        final var config = builder().build();

        assertFalse(config.enabled());
        assertFalse(config.submitMetrics());
        assertFalse(config.additionalMetrics());
        assertFalse(config.errorTracking());
    }

    @Test
    public void individualFeaturesCanBeDisabled() throws IOException {
        store(validConfig()
                .replace("submitMetrics=true", "submitMetrics=false")
                .replace("submitErrors=true", "submitErrors=false"));

        final var config = builder().build();

        assertTrue(config.enabled());
        assertFalse(config.submitMetrics());
        assertTrue(config.additionalMetrics());
        assertFalse(config.errorTracking());
    }

    @Test
    public void enabledFlagDisablesFeaturesWithoutChangingFile() throws IOException {
        System.setProperty("faststats.enabled", "false");

        final var config = builder().build();

        assertFalse(CustomConfig.enabledFlag());
        assertFalse(config.enabled());
        assertFalse(config.submitMetrics());
        assertFalse(config.additionalMetrics());
        assertFalse(config.errorTracking());
        assertEquals("true", load().getProperty("enabled"));
        assertEquals("true", load().getProperty("submitMetrics"));
    }

    @Test
    public void debugFlagIsNotPersisted() throws IOException {
        System.setProperty("faststats.debug", "true");

        final var config = builder().build();
        config.persist();

        assertTrue(config.debug());
        assertEquals("false", load().getProperty("debug"));
    }

    @Test
    public void enabledOverrideIsNotStored() throws IOException {
        final var config = builder().enabled(false).build();

        assertFalse(config.enabled());
        assertFalse(config.submitMetrics());
        assertFalse(load().containsKey("enabled"));

        config.persist();
        assertFalse(load().containsKey("enabled"));
    }

    @Test
    public void enabledOverrideIgnoresFile() throws IOException {
        store(validConfig().replace("enabled=true", "enabled=false"));

        final var config = builder().enabled(true).build();

        assertTrue(config.enabled());
        assertTrue(config.submitMetrics());
    }

    @Test
    public void persistWritesChangedSettings() throws IOException {
        final var config = builder().build();
        config.submitMetrics(false);
        config.errorTracking(false);
        config.additionalMetrics(false);
        config.persist();

        final var properties = load();
        assertEquals("false", properties.getProperty("submitMetrics"));
        assertEquals("false", properties.getProperty("submitErrors"));
        assertEquals("false", properties.getProperty("submitAdditionalMetrics"));
        assertEquals("true", properties.getProperty("enabled"));
    }

    @Test
    public void persistKeepsUnknownKeys() throws IOException {
        store(validConfig("unknown-key=value"));

        final var config = builder().build();
        config.persist();

        assertEquals("value", load().getProperty("unknown-key"));
    }

    @Test
    public void persistRecreatesDeletedFile() throws IOException {
        final var config = builder().field(SCREEN_SEEN).build();
        Files.delete(file);

        config.persist();

        final var properties = load();
        assertEquals(config.serverId().toString(), properties.getProperty("serverId"));
        assertEquals("3", properties.getProperty("configVersion"));
        assertEquals("false", properties.getProperty("onboarding-screen-seen"));
    }

    @Test
    public void persistUsesCustomComment() throws IOException {
        final var config = builder().comment("Custom").build();
        config.persist();

        assertEquals("#Custom", Files.readAllLines(file, UTF_8).get(0));
    }

    @Test
    public void setRejectsNull() {
        final var config = builder().field(SCREEN_SEEN).build();
        @SuppressWarnings("DataFlowIssue")
        final Runnable set = () -> config.set(SCREEN_SEEN, null);
        assertThrows(NullPointerException.class, set::run);
    }

    @Test
    public void differentFilesGetDifferentServerIds() {
        final var first = builder().build();
        final var second = CustomConfig.builder(directory.resolve("other.properties"), loggerFactory).build();

        assertNotEquals(first.serverId(), second.serverId());
    }

    @Test
    public void entriesAreSortedAlphabetically() throws IOException {
        store(validConfig("zeta=1", "Alpha=2", "beta=3", "counter=4"));

        final var config = builder().field(COUNTER).field(SCREEN_SEEN).build();
        assertKeysSorted();

        config.persist();
        assertKeysSorted();
    }

    @Test
    public void newFileIsSortedAlphabetically() throws IOException {
        builder().field(SCREEN_SEEN).field(COUNTER).build();

        assertKeysSorted();
    }

    private void assertKeysSorted() throws IOException {
        final var keys = Files.readAllLines(file, UTF_8).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.substring(0, line.indexOf('=')))
                .toList();
        final var sorted = keys.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        assertEquals(sorted, keys);
        assertFalse(keys.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "enabled ", " enabled", "on boarding", "a=b", "a:b", "#comment", "!comment",
            "-key", "key-", "_key", "key_", ".key", "key.", "a--b", "a-_b", "a..b",
            "k\u00E4se", "\uFF4B\uFF45\uFF59", "key\u00A0", "\u200Bkey", "key\u2060", "key\t", "key\n", "a/b", "a\\b"
    })
    @SuppressWarnings("PatternValidation")
    public void invalidFieldKeyIsRejected(final String key) {
        assertThrows(IllegalArgumentException.class, () -> CustomConfig.Field.bool(key, false));
        assertThrows(IllegalArgumentException.class, () -> CustomConfig.Field.string(key, ""));
        assertThrows(IllegalArgumentException.class, () -> new CustomConfig.Field<>(key, () -> "", value -> value, value -> value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"enabled", "submitMetrics", "consent-screen-shown", "onboarding_screen_seen", "a.b.c", "ENABLED", "v2", "1", "a-b_c.d"})
    public void validFieldKeyIsAccepted(final String key) {
        assertEquals(key, CustomConfig.Field.bool(key, false).key());
    }
}
