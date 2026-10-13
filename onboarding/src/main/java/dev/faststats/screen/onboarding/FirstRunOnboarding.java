package dev.faststats.screen.onboarding;

import dev.faststats.screen.registry.FastStatsRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

public final class FirstRunOnboarding {
    private static final AtomicBoolean OPENED = new AtomicBoolean();

    private FirstRunOnboarding() {
    }

    /**
     * @return whether the consent screen should be opened, only {@code true} once per launch
     */
    public static boolean shouldOpen() {
        final var registry = FastStatsRegistry.instance();
        if (!registry.consentRequired()) return false;
        if (!registry.firstRun()) return false;
        return OPENED.compareAndSet(false, true);
    }
}
