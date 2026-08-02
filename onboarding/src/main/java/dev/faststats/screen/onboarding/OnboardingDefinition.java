package dev.faststats.screen.onboarding;

import dev.faststats.FastStatsRegistry;
import dev.faststats.SubmissionSettings;
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
        final var config = registry.config();
        final var acceptAll = Button.button(Text.of("Accept All"))
                .enabled(!(config.submitMetrics() && config.additionalMetrics() && config.errorTracking()));
        final var declineAll = Button.button(Text.of("Decline All"))
                .enabled(config.submitMetrics() || config.errorTracking());
        final var collectedData = registrationText(
                        registry, config.submitMetrics(), config.additionalMetrics(), config.errorTracking())
                .box()
                .scrollable()
                .height(40);
        acceptAll.onClick((screen, button) -> setAll(screen, registry, button, declineAll, true));
        declineAll.onClick((screen, button) -> setAll(screen, registry, button, declineAll, false));

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
                                    updateRegistrationText(screen, registry, collectedData);
                                    save(screen, registry, acceptAll, declineAll);
                                }))
                        .add(Checkbox.create("submit_additional_metrics", Text.of("Submit Additional Metrics (provided by the developer)"))
                                .selected(config.additionalMetrics())
                                .enabled(config.submitMetrics())
                                .onStateChange((screen, self) -> {
                                    updateRegistrationText(screen, registry, collectedData);
                                    save(screen, registry, acceptAll, declineAll);
                                }))
                        .add(Checkbox.create("submit_errors", Text.of("Submit Errors"))
                                .selected(config.errorTracking())
                                .onStateChange((screen, self) -> {
                                    updateRegistrationText(screen, registry, collectedData);
                                    save(screen, registry, acceptAll, declineAll);
                                })))
                .addFooter(Division.div()
                        .gap(4)
                        .orientation(Division.Orientation.HORIZONTAL)
                        .add(declineAll)
                        .add(acceptAll)
                        .add(Button.button(Text.translatable("gui.done"))
                                .onClick((screen, button) -> screen.close())));
    }

    private static void setAll(final Screen screen, final FastStatsRegistry registry,
                               final Button acceptAll, final Button declineAll, final boolean selected) {
        screen.findSelect("submit_metrics").orElseThrow().selected(selected);
        screen.findSelect("submit_additional_metrics").orElseThrow()
                .enabled(selected)
                .selected(selected);
        screen.findSelect("submit_errors").orElseThrow().selected(selected);
        save(screen, registry, acceptAll, declineAll);
        screen.close();
    }

    private static void save(final Screen screen, final FastStatsRegistry registry,
                             final Button acceptAll, final Button declineAll) {
        final var metrics = screen.findSelect("submit_metrics")
                .map(Checkbox::value)
                .orElseThrow();
        final var additionalMetrics = screen.findSelect("submit_additional_metrics")
                .map(Checkbox::value)
                .orElseThrow();
        final var errors = screen.findSelect("submit_errors")
                .map(Checkbox::value)
                .orElseThrow();
        final var settings = new SubmissionSettings(metrics, additionalMetrics, errors);
        if (!update(registry, settings)) return;
        acceptAll.enabled(!(settings.submitMetrics() && settings.additionalMetrics() && settings.errorTracking()));
        declineAll.enabled(settings.submitMetrics() || settings.errorTracking());
        if (settings.submitMetrics() || settings.errorTracking()) registry.start();
        else registry.shutdown();
    }

    private static boolean update(final FastStatsRegistry registry, final SubmissionSettings settings) {
        try {
            registry.updateSubmissionSettings(settings);
            return true;
        } catch (final RuntimeException ignored) {
            return false;
        }
    }

    private static void updateRegistrationText(final Screen screen, final FastStatsRegistry registry,
                                               final ScrollableTextBox collectedData) {
        final var metrics = screen.findSelect("submit_metrics")
                .map(Checkbox::value)
                .orElseThrow();
        final var additionalMetrics = screen.findSelect("submit_additional_metrics")
                .map(Checkbox::value)
                .orElseThrow();
        final var errors = screen.findSelect("submit_errors")
                .map(Checkbox::value)
                .orElseThrow();
        collectedData.text(registrationText(registry, metrics, additionalMetrics, errors));
    }

    private static Text registrationText(final FastStatsRegistry registry,
                                         final boolean metricsEnabled,
                                         final boolean additionalMetricsEnabled,
                                         final boolean errorTrackingEnabled) {
        final var text = Text.of("> Installed mods that use FastStats:");
        final var registrations = registry.registrations();
        if (registrations.isEmpty()) {
            text.append(Text.of("\n  No registered mods").format(Text.Formatting.RED));
        } else registrations.forEach(registration -> {
            var projectName = Text.of(registration.projectName()).format(Text.Formatting.YELLOW);
            text.append("\n  ").append(projectName).append(":");
            if (registration.metrics()) {
                final var label = metricsEnabled
                        ? Text.of("Default Metrics").format(Text.Formatting.AQUA)
                        : Text.of("Default Metrics (disabled)").format(Text.Formatting.GRAY);
                text.append("\n  - ").append(label);
            }
            if (registration.errorTracking()) {
                final var label = errorTrackingEnabled
                        ? Text.of("Error Tracking").format(Text.Formatting.AQUA)
                        : Text.of("Error Tracking (disabled)").format(Text.Formatting.GRAY);
                text.append("\n  - ").append(label);
            }
            if (!registration.additionalMetrics().isEmpty()) {
                final var label = additionalMetricsEnabled
                        ? Text.of("Additional Metrics:").format(Text.Formatting.AQUA)
                        : Text.of("Additional Metrics (disabled):").format(Text.Formatting.GRAY);
                text.append("\n  - ").append(label);
                registration.additionalMetrics()
                        .stream().sorted()
                        .map(metric -> Text.of(metric).format(additionalMetricsEnabled
                                ? Text.Formatting.DARK_AQUA
                                : Text.Formatting.GRAY))
                        .forEach(metric -> text.append("\n    - ").append(metric));
            }
        });
        return text.append("\n\nLearn more: ")
                .append(Text.url("What data is collected?", INFO_URL));
    }
}
