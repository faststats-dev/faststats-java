package dev.faststats.screen;

import java.util.Optional;

public interface Screen {
    static Screen screen(Text title) {
        return new SimpleScreen(title);
    }

    Text title();
    
    Screen add(Element<?> element);

    Optional<Checkbox> findSelect(String id);

    void close();

    void open();
}
