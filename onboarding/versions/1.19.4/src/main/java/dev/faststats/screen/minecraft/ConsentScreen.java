package dev.faststats.screen.minecraft;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.faststats.screen.onboarding.ConsentState;
import dev.faststats.screen.onboarding.ConsentText;
import dev.faststats.screen.registry.FastStatsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
        super(Component.translatable(ConsentText.TITLE));
        this.parent = parent;
        this.firstRun = firstRun;
    }

    @Override
    protected void init() {
        final var layout = new HeaderAndFooterLayout(this, HEADER_AND_FOOTER_HEIGHT, HEADER_AND_FOOTER_HEIGHT);
        layout.addToHeader(new StringWidget(title, font));

        final int width = Math.min(this.width - MARGIN * 2, CONTENT_MAX_WIDTH);
        final var checkboxes = column();
        addBelow(checkboxes, checkbox(ConsentText.SUBMIT_METRICS, state.submitMetrics(), state.editable(), state::submitMetrics, width), 0);
        addBelow(checkboxes, checkbox(ConsentText.SUBMIT_ADDITIONAL_METRICS, state.additionalMetrics(), state.editable() && state.submitMetrics(), state::additionalMetrics, width), 4);
        addBelow(checkboxes, checkbox(ConsentText.SUBMIT_ERRORS, state.errorTracking(), state.editable(), state::errorTracking, width), 4);
        checkboxes.arrangeElements();

        final int textHeight = height - HEADER_AND_FOOTER_HEIGHT * 2 - checkboxes.getHeight() - SPACING * 3;
        text = new TextArea(font, component(ConsentText.create(state)), width, textHeight, this::clickLink);
        final var content = column();
        addBelow(content, text, 0);
        addBelow(content, checkboxes, SPACING);
        layout.addToContents(content);

        final var footer = row();
        addRight(footer, editable(Button.builder(Component.translatable(ConsentText.UNSELECT_ALL), button -> selectAll(false)).width(BUTTON_WIDTH).build()), 0);
        addRight(footer, editable(Button.builder(Component.translatable(ConsentText.SELECT_ALL), button -> selectAll(true)).width(BUTTON_WIDTH).build()), SPACING);
        addRight(footer, Button.builder(Component.translatable(ConsentText.DONE), button -> onClose()).width(BUTTON_WIDTH).build(), SPACING);
        layout.addToFooter(footer);

        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();
        text.scrollPosition(scroll);
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

    @Override
    public void render(final PoseStack graphics, final int mouseX, final int mouseY, final float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void refresh() {
        if (text != null) scroll = text.scrollPosition();
        rebuildWidgets();
    }

    private AbstractWidget checkbox(final String key, final boolean selected, final boolean enabled, final Consumer<Boolean> onChange, final int width) {
        final var checkbox = new Checkbox(0, 0, width, 20, Component.translatable(key), enabled && selected) {
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

    private static LinearLayout column() {
        return new LinearLayout(0, 0, LinearLayout.Orientation.VERTICAL);
    }

    private static LinearLayout row() {
        return new LinearLayout(0, 0, LinearLayout.Orientation.HORIZONTAL);
    }

    private static void addBelow(final LinearLayout layout, final LayoutElement child, final int gap) {
        layout.addChild(child, LayoutSettings.defaults().paddingTop(gap).alignHorizontallyLeft());
    }

    private static void addRight(final LinearLayout layout, final LayoutElement child, final int gap) {
        layout.addChild(child, LayoutSettings.defaults().paddingLeft(gap));
    }

    private static Component component(final List<ConsentText.Segment> segments) {
        final var component = Component.empty();
        segments.forEach(segment -> component.append(component(segment)));
        return component;
    }

    private static MutableComponent component(final ConsentText.Segment segment) {
        if (segment instanceof final ConsentText.Literal literal) {
            return color(Component.literal(literal.text()), literal.color());
        } else if (segment instanceof final ConsentText.Translation translation) {
            final var args = translation.args().stream().map(ConsentScreen::component).toArray();
            return color(Component.translatable(translation.key(), args), translation.color());
        }
        // links are stored as the insertion and opened by clickLink
        final var link = (ConsentText.Link) segment;
        return component(link.text())
                .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withInsertion(link.uri().toString()));
    }

    private static MutableComponent color(final MutableComponent component, final ConsentText.@Nullable Color color) {
        return color == null ? component : component.withStyle(ChatFormatting.valueOf(color.name()));
    }
}
