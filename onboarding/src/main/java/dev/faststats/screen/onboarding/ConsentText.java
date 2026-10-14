package dev.faststats.screen.onboarding;

import dev.faststats.screen.registry.FastStatsRegistry;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * The text and translation keys of the consent screen, described without Minecraft classes.
 * <p>
 * Every version turns the {@link Segment segments} into a component, links are shown in aqua and underlined.
 */
public final class ConsentText {
    public static final URI MODS_URL = URI.create("https://faststats.dev/mods");
    public static final URI ABUSE_URL = URI.create("https://faststats.dev/abuse");
    public static final URI INFO_URL = URI.create("https://faststats.dev/info");
    public static final URI PRIVACY_URL = URI.create("https://faststats.dev/privacy");

    public static final String BUTTON = "FastStats";
    public static final String TITLE = "faststats.consent.title";
    public static final String SUBMIT_METRICS = "faststats.consent.submit_metrics";
    public static final String SUBMIT_ADDITIONAL_METRICS = "faststats.consent.submit_additional_metrics";
    public static final String SUBMIT_ERRORS = "faststats.consent.submit_errors";
    public static final String SELECT_ALL = "faststats.consent.select_all";
    public static final String UNSELECT_ALL = "faststats.consent.unselect_all";
    public static final String DONE = "gui.done";

    private ConsentText() {
    }

    /**
     * @return the description and the mods using FastStats, reflecting the given consent
     */
    public static List<Segment> create(final ConsentState state) {
        final var text = new ArrayList<Segment>();
        text.add(new Translation("faststats.consent.description", null, List.of(
                new Link(new Literal("FastStats", null), MODS_URL),
                new Link(new Literal(ABUSE_URL.toString(), null), ABUSE_URL),
                new Link(new Literal(PRIVACY_URL.toString(), null), PRIVACY_URL))));
        text.add(new Literal("\n\n", null));
        text.add(new Translation("faststats.consent.installed_mods", null, List.of()));

        FastStatsRegistry.instance().registrations().forEach(registration -> {
            text.add(new Literal("\n  ", null));
            text.add(new Literal(registration.projectName(), Color.YELLOW));
            text.add(new Literal(":", null));
            if (registration.metrics()) {
                feature(text, "faststats.consent.default_metrics", state.submitMetrics());
            }
            if (registration.errorTracking()) {
                feature(text, "faststats.consent.error_tracking", state.errorTracking());
            }
            if (!registration.additionalMetrics().isEmpty()) {
                feature(text, "faststats.consent.additional_metrics", state.additionalMetrics());
                registration.additionalMetrics().stream().sorted().forEach(metric -> {
                    text.add(new Literal("\n    - ", null));
                    text.add(new Literal(metric, state.additionalMetrics() ? Color.DARK_AQUA : Color.GRAY));
                });
            }
        });

        text.add(new Literal("\n\n", null));
        text.add(new Translation("faststats.consent.learn_more", null, List.of(
                new Link(new Translation("faststats.consent.learn_more.link", null, List.of()), INFO_URL))));
        return text;
    }

    private static void feature(final List<Segment> text, final String key, final boolean enabled) {
        text.add(new Literal("\n  - ", null));
        text.add(enabled
                ? new Translation(key, Color.AQUA, List.of())
                : new Translation(key + ".disabled", Color.GRAY, List.of()));
    }

    /**
     * The colors used by the consent text, named like Minecraft's {@code ChatFormatting}.
     */
    public enum Color {
        AQUA, DARK_AQUA, GRAY, YELLOW
    }

    public sealed interface Segment permits Literal, Translation, Link {
    }

    public record Literal(String text, @Nullable Color color) implements Segment {
    }

    public record Translation(String key, @Nullable Color color, List<Segment> args) implements Segment {
    }

    public record Link(Segment text, URI uri) implements Segment {
    }
}
