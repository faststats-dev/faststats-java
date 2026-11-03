package dev.faststats.screen.onboarding;

import dev.faststats.config.CustomConfig;
import dev.faststats.screen.registry.FastStatsRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

public final class FirstRunOnboarding {
    private static final AtomicBoolean OPENED = new AtomicBoolean();

    private FirstRunOnboarding() {
    }

    public static boolean shouldOpen() {
        if (!CustomConfig.enabledFlag()) return false;
        if (!FastStatsRegistry.instance().consentPending()) return false;
        return OPENED.compareAndSet(false, true);
    }
}
