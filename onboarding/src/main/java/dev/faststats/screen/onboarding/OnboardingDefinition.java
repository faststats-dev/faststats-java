package dev.faststats.screen.onboarding;

import dev.faststats.FastStatsRegistry;
import dev.faststats.config.SimpleConfig;
import dev.faststats.screen.Button;
import dev.faststats.screen.Checkbox;
import dev.faststats.screen.Division;
import dev.faststats.screen.Screen;
import dev.faststats.screen.ScrollableTextBox;
import dev.faststats.screen.Text;

import java.net.URI;

public final class OnboardingDefinition {
    public static final URI MODS_URL = URI.create("https://faststats.dev/mods");
    public static final URI ABUSE_URL = URI.create("https://faststats.dev/abuse");
    public static final URI INFO_URL = URI.create("https://faststats.dev/info");
    public static final URI PRIVACY_URL = URI.create("https://faststats.dev/privacy");

    public static Screen create() {
        final var registry = FastStatsRegistry.instance();
        final var config = ((SimpleConfig) registry.config());
        final var acceptAll = Button.button(Text.of("Select All"));
        final var declineAll = Button.button(Text.of("Unselect All"));

        final var collectedData = registrationText().box().scrollable().height(40);

        acceptAll.onClick((screen, button) -> setAll(screen, true, collectedData));
        declineAll.onClick((screen, button) -> setAll(screen, false, collectedData));

        return Screen.screen(Text.of("FastStats Metrics"))
                .add(Text.text()
                        .append(Text.url("FastStats", MODS_URL))
                        .append(" collects anonymous usage statistics and errors.\n")
                        .append("Keeping Metrics and Error tracking enabled helps developers to improve their mods.\n\n")
                        .append("If you suspect a developer is collecting personal data or bypassing any opt-out option,\n")
                        .append("please report it at: ")
                        .append(Text.url(ABUSE_URL))
                        .append("\n\nPrivacy policy: ")
                        .append(Text.url(PRIVACY_URL))
                        .box()
                        .scrollable()
                        .height(30))
                .add(collectedData)
                .addBottom(Division.div()
                        .gap(4)
                        .add(Checkbox.create("submit_metrics", Text.of("Submit Metrics"))
                                .selected(config.submitMetrics())
                                .onStateChange((screen, self) -> {
                                    final var select = screen.findSelect("submit_additional_metrics").orElseThrow();
                                    select.enabled(self.selected());
                                    update(screen, collectedData);
                                }))
                        .add(Checkbox.create("submit_additional_metrics", Text.of("Submit Additional Metrics (provided by the developer)"))
                                .selected(config.additionalMetrics())
                                .enabled(config.submitMetrics())
                                .onStateChange((screen, self) -> {
                                    update(screen, collectedData);
                                }))
                        .add(Checkbox.create("submit_errors", Text.of("Submit Errors"))
                                .selected(config.errorTracking())
                                .onStateChange((screen, self) -> {
                                    update(screen, collectedData);
                                })))
                .addFooter(Division.div()
                        .gap(4)
                        .orientation(Division.Orientation.HORIZONTAL)
                        .add(declineAll)
                        .add(acceptAll)
                        .add(Button.button(Text.translatable("gui.done"))
                                .onClick((screen, button) -> screen.close())))
                .onClose(config::persist);
    }

    private static void setAll(final Screen screen, final boolean selected, final ScrollableTextBox collectedData) {
        screen.findSelect("submit_metrics").orElseThrow().selected(selected);
        screen.findSelect("submit_additional_metrics").orElseThrow()
                .enabled(selected)
                .selected(selected);
        screen.findSelect("submit_errors").orElseThrow().selected(selected);
        update(screen, collectedData);
        screen.open();
    }

    private static void update(final Screen screen, final ScrollableTextBox collectedData) {
        final var metrics = screen.findSelect("submit_metrics")
                .map(Checkbox::value)
                .orElseThrow();
        final var additionalMetrics = screen.findSelect("submit_additional_metrics")
                .map(Checkbox::value)
                .orElseThrow();
        final var errors = screen.findSelect("submit_errors")
                .map(Checkbox::value)
                .orElseThrow();

        final var config = (SimpleConfig) FastStatsRegistry.instance().config();
        config.errorTracking(errors);
        config.additionalMetrics(additionalMetrics);
        config.submitMetrics(metrics);
        config.enabled(errors || additionalMetrics || metrics);

        if (metrics || errors) FastStatsRegistry.instance().start();
        else FastStatsRegistry.instance().shutdown();

        updateRegistrationText(screen, collectedData);
    }

    private static void updateRegistrationText(final Screen screen, final ScrollableTextBox collectedData) {
        final var metrics = screen.findSelect("submit_metrics")
                .map(Checkbox::value)
                .orElseThrow();
        final var additionalMetrics = screen.findSelect("submit_additional_metrics")
                .map(Checkbox::value)
                .orElseThrow();
        final var errors = screen.findSelect("submit_errors")
                .map(Checkbox::value)
                .orElseThrow();
        collectedData.text(registrationText());
    }

    private static Text registrationText() {
        final var config = FastStatsRegistry.instance().config();
        final var text = Text.of("> Installed mods that use FastStats:");
        final var registrations = FastStatsRegistry.instance().registrations();
        if (registrations.isEmpty()) {
            text.append(Text.of("\n  No registered mods").format(Text.Formatting.RED));
        } else registrations.forEach(registration -> {
            final var projectName = Text.of(registration.projectName()).format(Text.Formatting.YELLOW);
            text.append("\n  ").append(projectName).append(":");
            if (registration.metrics()) {
                final var label = config.submitMetrics()
                        ? Text.of("Default Metrics").format(Text.Formatting.AQUA)
                        : Text.of("Default Metrics (disabled)").format(Text.Formatting.GRAY);
                text.append("\n  - ").append(label);
            }
            if (registration.errorTracking()) {
                final var label = config.errorTracking()
                        ? Text.of("Error Tracking").format(Text.Formatting.AQUA)
                        : Text.of("Error Tracking (disabled)").format(Text.Formatting.GRAY);
                text.append("\n  - ").append(label);
            }
            if (!registration.additionalMetrics().isEmpty()) {
                final var label = config.additionalMetrics()
                        ? Text.of("Additional Metrics:").format(Text.Formatting.AQUA)
                        : Text.of("Additional Metrics (disabled):").format(Text.Formatting.GRAY);
                text.append("\n  - ").append(label);
                registration.additionalMetrics()
                        .stream().sorted()
                        .map(metric -> Text.of(metric).format(config.additionalMetrics()
                                ? Text.Formatting.DARK_AQUA
                                : Text.Formatting.GRAY))
                        .forEach(metric -> text.append("\n    - ").append(metric));
            }
        });
        return text.append("\n\nLearn more: ")
                .append(Text.url("What data is collected?", INFO_URL));
    }
}
