package dev.faststats.screen.onboarding;

import dev.faststats.screen.Button;
import dev.faststats.screen.Checkbox;
import dev.faststats.screen.Division;
import dev.faststats.screen.Screen;
import dev.faststats.screen.ScrollableTextBox;
import dev.faststats.screen.Text;
import dev.faststats.screen.registry.FastStatsRegistry;

import java.net.URI;

public final class OnboardingDefinition {
    public static final URI MODS_URL = URI.create("https://faststats.dev/mods");
    public static final URI ABUSE_URL = URI.create("https://faststats.dev/abuse");
    public static final URI INFO_URL = URI.create("https://faststats.dev/info");
    public static final URI PRIVACY_URL = URI.create("https://faststats.dev/privacy");

    public static Screen create() {
        final var registry = FastStatsRegistry.instance();
        final var state = new ConsentState(registry.anyConfig());
        final var acceptAll = Button.button(Text.translatable("faststats.consent.select_all"));
        final var declineAll = Button.button(Text.translatable("faststats.consent.unselect_all"));

        final var collectedData = consentText(state).box().scrollable();

        acceptAll.onClick((screen, button) -> setAll(screen, state, true, collectedData));
        declineAll.onClick((screen, button) -> setAll(screen, state, false, collectedData));

        return Screen.screen(Text.translatable("faststats.consent.title"))
                .add(collectedData)
                .addBottom(Division.div()
                        .gap(4)
                        .add(Checkbox.create("submit_metrics", Text.translatable("faststats.consent.submit_metrics"))
                                .selected(state.submitMetrics())
                                .onStateChange((screen, self) -> {
                                    final var select = screen.findSelect("submit_additional_metrics").orElseThrow();
                                    state.submitMetrics(self.selected());
                                    select.enabled(self.selected());
                                    if (!self.selected()) select.selected(false);
                                    update(state, collectedData);
                                }))
                        .add(Checkbox.create("submit_additional_metrics", Text.translatable("faststats.consent.submit_additional_metrics"))
                                .selected(state.additionalMetrics())
                                .enabled(state.submitMetrics())
                                .onStateChange((screen, self) -> {
                                    state.additionalMetrics(self.value());
                                    update(state, collectedData);
                                }))
                        .add(Checkbox.create("submit_errors", Text.translatable("faststats.consent.submit_errors"))
                                .selected(state.errorTracking())
                                .onStateChange((screen, self) -> {
                                    state.errorTracking(self.selected());
                                    update(state, collectedData);
                                })))
                .addFooter(Division.div()
                        .gap(4)
                        .orientation(Division.Orientation.HORIZONTAL)
                        .add(declineAll)
                        .add(acceptAll)
                        .add(Button.button(Text.translatable("gui.done"))
                                .onClick((screen, button) -> screen.close())))
                .onClose(() -> state.apply(registry));
    }

    private static void setAll(final Screen screen, final ConsentState state, final boolean selected,
                               final ScrollableTextBox collectedData) {
        state.selectAll(selected);
        screen.findSelect("submit_metrics").orElseThrow().selected(selected);
        screen.findSelect("submit_additional_metrics").orElseThrow()
                .enabled(selected)
                .selected(selected);
        screen.findSelect("submit_errors").orElseThrow().selected(selected);
        update(state, collectedData);
        screen.open();
    }

    private static void update(final ConsentState state, final ScrollableTextBox collectedData) {
        updateRegistrationText(state, collectedData);
    }

    private static void updateRegistrationText(final ConsentState state, final ScrollableTextBox collectedData) {
        collectedData.text(consentText(state));
    }

    private static Text consentText(final ConsentState state) {
        return Text.translatable("faststats.consent.description",
                        Text.url("FastStats", MODS_URL), Text.url(ABUSE_URL), Text.url(PRIVACY_URL))
                .append("\n\n")
                .append(registrationText(state));
    }

    private static Text registrationText(final ConsentState state) {
        final var text = Text.translatable("faststats.consent.installed_mods");
        FastStatsRegistry.instance().registrations().forEach(registration -> {
            final var projectName = Text.of(registration.projectName()).format(Text.Formatting.YELLOW);
            text.append("\n  ").append(projectName).append(":");
            if (registration.metrics()) {
                final var label = state.submitMetrics()
                        ? Text.translatable("faststats.consent.default_metrics").format(Text.Formatting.AQUA)
                        : Text.translatable("faststats.consent.default_metrics.disabled").format(Text.Formatting.GRAY);
                text.append("\n  - ").append(label);
            }
            if (registration.errorTracking()) {
                final var label = state.errorTracking()
                        ? Text.translatable("faststats.consent.error_tracking").format(Text.Formatting.AQUA)
                        : Text.translatable("faststats.consent.error_tracking.disabled").format(Text.Formatting.GRAY);
                text.append("\n  - ").append(label);
            }
            if (!registration.additionalMetrics().isEmpty()) {
                final var label = state.additionalMetrics()
                        ? Text.translatable("faststats.consent.additional_metrics").format(Text.Formatting.AQUA)
                        : Text.translatable("faststats.consent.additional_metrics.disabled").format(Text.Formatting.GRAY);
                text.append("\n  - ").append(label);
                registration.additionalMetrics()
                        .stream().sorted()
                        .map(metric -> Text.of(metric).format(state.additionalMetrics()
                                ? Text.Formatting.DARK_AQUA
                                : Text.Formatting.GRAY))
                        .forEach(metric -> text.append("\n    - ").append(metric));
            }
        });
        return text.append("\n\n").append(Text.translatable("faststats.consent.learn_more",
                Text.url(Text.translatable("faststats.consent.learn_more.link"), INFO_URL)));
    }
}
