package dev.faststats.screen.onboarding;

import dev.faststats.screen.Button;
import dev.faststats.screen.Checkbox;
import dev.faststats.screen.Division;
import dev.faststats.screen.Screen;
import dev.faststats.screen.Text;

import java.net.URI;

public final class OnboardingDefinition {
    public static final URI MODS_URL = URI.create("https://faststats.dev/mods");
    public static final URI ABUSE_URL = URI.create("https://faststats.dev/abuse");
    public static final URI INFO_URL = URI.create("https://faststats.dev/info");

    public static Screen create() {
        return Screen.screen(Text.of("FastStats Metrics"))
                .add(Text.text()
                        .append(Text.url("FastStats", MODS_URL))
                        .append(" collects anonymous usage statistics and errors.\n")
                        .append("Keeping Metrics and Error tracking enabled helps developers to improve their mods.\n\n")
                        .append("If you suspect a developer is collecting personal data or bypassing any opt-out option,\n")
                        .append("please report it at: ")
                        .append(Text.url(ABUSE_URL))
                        .box())
                .add(Checkbox.create("submit_metrics", Text.of("Submit Metrics")).onStateChange((screen, self) -> {
                    final var select = screen.findSelect("submit_additional_metrics").orElseThrow();
                    select.enabled(self.selected());
                }))
                .add(Checkbox.create("submit_additional_metrics", Text.of("Submit Additional Metrics (provided by the developer)")))
                .add(Checkbox.create("submit_errors", Text.of("Submit Errors")))
                .add(Division.div()
                        .gap(4)
                        .orientation(Division.Orientation.HORIZONTAL)
                        .add(Button.button(Text.of("Decline All").format(Text.Formatting.RED))
                                .onClick(screen -> {
                                    screen.close();
                                    // todo: store changed values
                                }))
                        .add(Button.button(Text.of("Confirm Selection").format(Text.Formatting.GREEN))
                                .onClick(screen -> {
                                    final var metrics = screen.findSelect("submit_metrics")
                                            .map(Checkbox::value)
                                            .orElseThrow();
                                    final var additionalMetrics = screen.findSelect("submit_additional_metrics")
                                            .map(Checkbox::value)
                                            .orElseThrow();
                                    final var errors = screen.findSelect("submit_errors")
                                            .map(Checkbox::value)
                                            .orElseThrow();
                                    // todo: store selected values
                                    screen.close();
                                })));

        // todo: list all mods and collected values

        //         List.of("Mod version", "Fabric/NeoForge version", "..."),
        //         List.of(
        //                 new TrackedMod("TreeHugger69", List.of("Default Metrics", "Error Tracking"), List.of("client_age", "language")),
        //                 new TrackedMod("Sucker123", List.of("Default Metrics"), List.of())
        //         ),
    }
}
