package dev.faststats.screen;

import java.util.ServiceLoader;

public interface ScreenManager {
    static ScreenManager instance() {
        final class Holder {
            private static final ScreenManager INSTANCE = ServiceLoader.load(ScreenManager.class)
                    .findFirst()
                    .orElseThrow();
        }
        return Holder.INSTANCE;
    }

    Text newText();

    void closeScreen(Screen screen);

    void openScreen(Screen screen);
}
