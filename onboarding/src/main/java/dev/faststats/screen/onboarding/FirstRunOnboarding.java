package dev.faststats.screen.onboarding;

import dev.faststats.screen.registry.FastStatsRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

public final class FirstRunOnboarding {
    private static final AtomicBoolean OPENED = new AtomicBoolean();

    private FirstRunOnboarding() {
    }

    public static boolean openIfNeeded() {
        final var registry = FastStatsRegistry.instance();
        if (registry.registrations().isEmpty()) return false;
        if (!registry.firstRun()) return false;
        if (!OPENED.compareAndSet(false, true)) return false;
        OnboardingDefinition.create().open();
        return true;
    }
}
