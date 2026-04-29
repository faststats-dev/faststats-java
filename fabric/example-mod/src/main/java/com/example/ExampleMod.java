package com.example;

import dev.faststats.ErrorTracker;
import dev.faststats.Metrics;
import dev.faststats.data.Metric;
import dev.faststats.fabric.FabricContext;
import net.fabricmc.api.ModInitializer;

public class ExampleMod implements ModInitializer {
    private final FabricContext context = new FabricContext(
            "example-mod", // your mod id as defined in fabric.mod.json
            "YOUR_TOKEN_HERE"
    );
    private final Metrics metrics = context.metrics()
            // Custom metrics require a corresponding data source in your project settings
            .addMetric(Metric.number("example_metric", () -> 42))

            // Error tracking must be enabled in the project settings
            .errorTracker(ErrorTracker.contextAware())

            .create();

    @Override
    public void onInitialize() {
    }
}
