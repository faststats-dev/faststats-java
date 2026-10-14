package dev.faststats.screen.minecraft;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.faststats.screen.onboarding.ConsentState;
import dev.faststats.screen.onboarding.ConsentText;
import dev.faststats.screen.registry.FastStatsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

public final class ConsentScreen extends Screen {
    private static final float DISABLED_ALPHA = 0.45F;
    private static final int BUTTON_WIDTH = 100;
    private static final int CONTENT_MAX_WIDTH = 600;
    private static final int HEADER_AND_FOOTER_HEIGHT = 33;
    private static final int MARGIN = 20;
    private static final int SPACING = 8;

    private final @Nullable Screen parent;
    private final boolean firstRun;
    private final ConsentState state = new ConsentState(FastStatsRegistry.instance().anyConfig());
    private @Nullable TextArea text;
    private double scroll;

    public ConsentScreen(final @Nullable Screen parent) {
        this(parent, false);
    }

    public ConsentScreen(final @Nullable Screen parent, final boolean firstRun) {
        super(new TranslatableComponent(ConsentText.TITLE));
        this.parent = parent;
        this.firstRun = firstRun;
    }

    @Override
    protected void init() {
        final int width = Math.min(this.width - MARGIN * 2, CONTENT_MAX_WIDTH);
        final int left = (this.width - width) / 2;

        final int buttonsLeft = (this.width - BUTTON_WIDTH * 3 - SPACING * 2) / 2;
        final int buttonsY = height - (HEADER_AND_FOOTER_HEIGHT + 20) / 2;
        addRenderableWidget(editable(new Button(buttonsLeft, buttonsY, BUTTON_WIDTH, 20, new TranslatableComponent(ConsentText.UNSELECT_ALL), button -> selectAll(false))));
        addRenderableWidget(editable(new Button(buttonsLeft + BUTTON_WIDTH + SPACING, buttonsY, BUTTON_WIDTH, 20, new TranslatableComponent(ConsentText.SELECT_ALL), button -> selectAll(true))));
        addRenderableWidget(new Button(buttonsLeft + (BUTTON_WIDTH + SPACING) * 2, buttonsY, BUTTON_WIDTH, 20, new TranslatableComponent(ConsentText.DONE), button -> onClose()));

        final int checkboxesY = height - HEADER_AND_FOOTER_HEIGHT - SPACING - 20 * 3 - 4 * 2;
        addRenderableWidget(checkbox(ConsentText.SUBMIT_METRICS, left, checkboxesY, state.submitMetrics(), state.editable(), state::submitMetrics));
        addRenderableWidget(checkbox(ConsentText.SUBMIT_ADDITIONAL_METRICS, left, checkboxesY + 24, state.additionalMetrics(), state.editable() && state.submitMetrics(), state::additionalMetrics));
        addRenderableWidget(checkbox(ConsentText.SUBMIT_ERRORS, left, checkboxesY + 48, state.errorTracking(), state.editable(), state::errorTracking));

        final int textY = HEADER_AND_FOOTER_HEIGHT + SPACING;
        text = addRenderableWidget(new TextArea(font, component(ConsentText.create(state)), left, textY, width, checkboxesY - SPACING - textY, this::clickLink));
        text.scrollPosition(scroll);
    }

    @Override
    public void render(final PoseStack pose, final int mouseX, final int mouseY, final float delta) {
        renderBackground(pose);
        drawCenteredString(pose, font, title, width / 2, (HEADER_AND_FOOTER_HEIGHT - font.lineHeight) / 2, 0xFFFFFF);
        super.render(pose, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !firstRun;
    }

    @Override
    public void onClose() {
        state.apply(FastStatsRegistry.instance());
        minecraft.setScreen(parent);
    }

    private void refresh() {
        if (text != null) scroll = text.scrollPosition();
        init(minecraft, width, height);
    }

    private Checkbox checkbox(final String key, final int x, final int y, final boolean selected, final boolean enabled, final Consumer<Boolean> onChange) {
        final Component label = new TranslatableComponent(key);
        final var checkbox = new Checkbox(x, y, 24 + font.width(label), 20, label, enabled && selected) {
            @Override
            public void onPress() {
                super.onPress();
                onChange.accept(selected());
                refresh();
            }
        };
        checkbox.active = enabled;
        checkbox.setAlpha(enabled ? 1.0F : DISABLED_ALPHA);
        return checkbox;
    }

    private Button editable(final Button button) {
        button.active = state.editable();
        return button;
    }

    private void selectAll(final boolean selected) {
        state.selectAll(selected);
        refresh();
    }

    private boolean clickLink(final @Nullable Style style) {
        final var link = style == null ? null : style.getInsertion();
        if (link == null) return false;
        final var uri = URI.create(link);
        minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) Util.getPlatform().openUri(uri);
            minecraft.setScreen(this);
        }, link, true));
        return true;
    }

    private static Component component(final List<ConsentText.Segment> segments) {
        final MutableComponent component = new TextComponent("");
        segments.forEach(segment -> component.append(component(segment)));
        return component;
    }

    private static MutableComponent component(final ConsentText.Segment segment) {
        if (segment instanceof final ConsentText.Literal literal) {
            return color(new TextComponent(literal.text()), literal.color());
        } else if (segment instanceof final ConsentText.Translation translation) {
            final var args = translation.args().stream().map(ConsentScreen::component).toArray();
            return color(new TranslatableComponent(translation.key(), args), translation.color());
        }
        final var link = (ConsentText.Link) segment;
        return component(link.text())
                .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withInsertion(link.uri().toString()));
    }

    private static MutableComponent color(final MutableComponent component, final ConsentText.@Nullable Color color) {
        return color == null ? component : component.withStyle(ChatFormatting.valueOf(color.name()));
    }
}
