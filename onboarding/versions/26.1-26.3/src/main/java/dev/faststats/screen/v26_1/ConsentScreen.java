package dev.faststats.screen.v26_1;

import dev.faststats.screen.onboarding.ConsentState;
import dev.faststats.screen.onboarding.ConsentText;
import dev.faststats.screen.registry.FastStatsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public final class ConsentScreen extends Screen {
    private static final float DISABLED_ALPHA = 0.45F;
    private static final int BUTTON_WIDTH = 100;
    private static final int CONTENT_MAX_WIDTH = 600;
    private static final int MARGIN = 20;
    private static final int SPACING = 8;

    private final @Nullable Screen parent;
    private final ConsentState state = new ConsentState(FastStatsRegistry.instance().anyConfig());
    private @Nullable TextArea text;
    private double scroll;

    public ConsentScreen(final @Nullable Screen parent) {
        super(Component.translatable(ConsentText.TITLE));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // an empty list draws the darker background and the separators like vanilla's list screens
        addRenderableOnly(new ObjectSelectionList<Empty>(minecraft, this.width, height - HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT * 2,
                HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT, 1) {
        });

        final var layout = new HeaderAndFooterLayout(this);
        layout.addTitleHeader(title, font);

        final int width = Math.min(this.width - MARGIN * 2, CONTENT_MAX_WIDTH);
        final var checkboxes = LinearLayout.vertical().spacing(4);
        checkboxes.addChild(checkbox(ConsentText.SUBMIT_METRICS, state.submitMetrics(), true, state::submitMetrics, width));
        checkboxes.addChild(checkbox(ConsentText.SUBMIT_ADDITIONAL_METRICS, state.additionalMetrics(), state.submitMetrics(), state::additionalMetrics, width));
        checkboxes.addChild(checkbox(ConsentText.SUBMIT_ERRORS, state.errorTracking(), true, state::errorTracking, width));
        checkboxes.arrangeElements();

        final int textHeight = layout.getContentHeight() - checkboxes.getHeight() - SPACING * 3;
        text = new TextArea(font, component(ConsentText.create(state)), width, textHeight, style -> {
            if (style.getClickEvent() != null) defaultHandleClickEvent(style.getClickEvent(), minecraft, this);
        });
        final var content = LinearLayout.vertical().spacing(SPACING);
        content.defaultCellSetting().alignHorizontallyLeft();
        content.addChild(text);
        content.addChild(checkboxes);
        layout.addToContents(content, settings -> settings.paddingBottom(SPACING));

        final var footer = layout.addToFooter(LinearLayout.horizontal().spacing(SPACING));
        footer.addChild(Button.builder(Component.translatable(ConsentText.UNSELECT_ALL), button -> selectAll(false)).width(BUTTON_WIDTH).build());
        footer.addChild(Button.builder(Component.translatable(ConsentText.SELECT_ALL), button -> selectAll(true)).width(BUTTON_WIDTH).build());
        footer.addChild(Button.builder(Component.translatable(ConsentText.DONE), button -> onClose()).width(BUTTON_WIDTH).build());

        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();
        text.setScrollAmount(scroll);
    }

    @Override
    protected void rebuildWidgets() {
        if (text != null) scroll = text.scrollAmount();
        super.rebuildWidgets();
    }

    @Override
    public void onClose() {
        state.apply(FastStatsRegistry.instance());
        minecraft.setScreenAndShow(parent);
    }

    private Checkbox checkbox(final String key, final boolean selected, final boolean enabled, final Consumer<Boolean> onChange, final int width) {
        final var checkbox = Checkbox.builder(Component.translatable(key), font)
                .maxWidth(width)
                .selected(enabled && selected)
                .onValueChange((self, value) -> {
                    onChange.accept(value);
                    screenExecutor.execute(this::rebuildWidgets);
                })
                .build();
        checkbox.active = enabled;
        checkbox.setAlpha(enabled ? 1.0F : DISABLED_ALPHA);
        return checkbox;
    }

    private void selectAll(final boolean selected) {
        state.selectAll(selected);
        rebuildWidgets();
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
        final var link = (ConsentText.Link) segment;
        return component(link.text()).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(link.uri())));
    }

    private static MutableComponent color(final MutableComponent component, final ConsentText.@Nullable Color color) {
        return color == null ? component : component.withStyle(ChatFormatting.valueOf(color.name()));
    }

    private abstract static class Empty extends ObjectSelectionList.Entry<Empty> {
    }
}
