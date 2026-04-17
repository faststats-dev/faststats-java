package com.example;

import dev.faststats.core.ErrorTracker;
import dev.faststats.core.Metrics;
import dev.faststats.core.Settings;
import dev.faststats.core.data.Metric;
import dev.faststats.fabric.FabricMetrics;
import net.fabricmc.api.ModInitializer;

public class ExampleMod implements ModInitializer {
    private final Metrics metrics = FabricMetrics.factory()
            // Custom metrics require a corresponding data source in your project settings
            .addMetric(Metric.number("example_metric", () -> 42))

            // Error tracking must be enabled in the project settings
            .errorTracker(ErrorTracker.contextAware())

            .settings(Settings.withToken("YOUR_TOKEN_HERE")) // token can be found in the settings of your project
            .create("example-mod"); // your mod id as defined in fabric.mod.json

    @Override
    public void onInitialize() {
    }
}
