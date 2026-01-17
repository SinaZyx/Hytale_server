package com.fancyinnovations.uihelper;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Fluent API for building Hytale UIs.
 * Makes UI code more readable and maintainable.
 *
 * Example usage:
 * <pre>
 * UIBuilder ui = new UIBuilder(commandBuilder, eventBuilder);
 * ui.page("Pages/MyPlugin/MyPage.ui")
 *   .set("#Title.Text", "Hello World")
 *   .set("#Counter.Text", String.valueOf(count))
 *   .onClick("#SaveButton", "Save")
 *   .onInput("#NameInput", "@Name")
 *   .list("#Items", items, (item, row) -> {
 *       row.set("#Name.Text", item.getName())
 *          .set("#Icon.Background", item.getColor())
 *          .onClick("#EditButton", "Edit:" + item.getId());
 *   });
 * </pre>
 */
public class UIBuilder {

    @Nonnull
    private final UICommandBuilder commands;

    @Nonnull
    private final UIEventBuilder events;

    @Nullable
    private String currentPrefix = null;

    public UIBuilder(@Nonnull UICommandBuilder commands, @Nonnull UIEventBuilder events) {
        this.commands = commands;
        this.events = events;
    }

    /**
     * Creates a new UIBuilder with fresh builders.
     */
    public UIBuilder() {
        this(new UICommandBuilder(), new UIEventBuilder());
    }

    /**
     * Appends a UI page template.
     */
    @Nonnull
    public UIBuilder page(@Nonnull String templatePath) {
        commands.append(templatePath);
        return this;
    }

    /**
     * Appends a UI template to a parent element.
     */
    @Nonnull
    public UIBuilder append(@Nonnull String parent, @Nonnull String templatePath) {
        commands.append(parent, templatePath);
        return this;
    }

    /**
     * Appends inline UI definition.
     */
    @Nonnull
    public UIBuilder appendInline(@Nonnull String parent, @Nonnull String uiDefinition) {
        commands.appendInline(parent, uiDefinition);
        return this;
    }

    /**
     * Clears children of an element.
     */
    @Nonnull
    public UIBuilder clear(@Nonnull String selector) {
        commands.clear(selector);
        return this;
    }

    /**
     * Removes an element from the UI.
     */
    @Nonnull
    public UIBuilder remove(@Nonnull String selector) {
        commands.remove(selector);
        return this;
    }

    /**
     * Sets an element property (String value).
     */
    @Nonnull
    public UIBuilder set(@Nonnull String selectorAndProperty, @Nonnull String value) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selectorAndProperty : selectorAndProperty;
        commands.set(fullSelector, value);
        return this;
    }

    /**
     * Sets an element property (float value).
     */
    @Nonnull
    public UIBuilder set(@Nonnull String selectorAndProperty, float value) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selectorAndProperty : selectorAndProperty;
        commands.set(fullSelector, value);
        return this;
    }

    /**
     * Sets an element property (int value).
     */
    @Nonnull
    public UIBuilder set(@Nonnull String selectorAndProperty, int value) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selectorAndProperty : selectorAndProperty;
        commands.set(fullSelector, value);
        return this;
    }

    /**
     * Sets an element property (boolean value).
     */
    @Nonnull
    public UIBuilder set(@Nonnull String selectorAndProperty, boolean value) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selectorAndProperty : selectorAndProperty;
        commands.set(fullSelector, value);
        return this;
    }

    /**
     * Sets text content of an element.
     */
    @Nonnull
    public UIBuilder text(@Nonnull String selector, @Nonnull String text) {
        return set(selector + ".Text", text);
    }

    /**
     * Sets background color of an element.
     */
    @Nonnull
    public UIBuilder background(@Nonnull String selector, @Nonnull String color) {
        return set(selector + ".Background", color);
    }

    /**
     * Sets the value of a ProgressBar element (0.0 to 1.0).
     */
    @Nonnull
    public UIBuilder progress(@Nonnull String selector, float value) {
        return set(selector + ".Value", value);
    }

    /**
     * Sets visibility of an element.
     */
    @Nonnull
    public UIBuilder visible(@Nonnull String selector, boolean visible) {
        return set(selector + ".Visible", visible);
    }

    /**
     * Shows an element (sets Visible: true).
     */
    @Nonnull
    public UIBuilder show(@Nonnull String selector) {
        return visible(selector, true);
    }

    /**
     * Hides an element (sets Visible: false).
     */
    @Nonnull
    public UIBuilder hide(@Nonnull String selector) {
        return visible(selector, false);
    }

    /**
     * Conditionally shows element.
     */
    @Nonnull
    public UIBuilder showIf(@Nonnull String selector, boolean condition) {
        return visible(selector, condition);
    }

    /**
     * Conditionally hides element.
     */
    @Nonnull
    public UIBuilder hideIf(@Nonnull String selector, boolean condition) {
        return visible(selector, !condition);
    }

    /**
     * Sets the Disabled property.
     */
    @Nonnull
    public UIBuilder disabled(@Nonnull String selector, boolean disabled) {
        return set(selector + ".Disabled", disabled);
    }

    /**
     * Disables an element.
     */
    @Nonnull
    public UIBuilder disable(@Nonnull String selector) {
        return disabled(selector, true);
    }

    /**
     * Enables an element.
     */
    @Nonnull
    public UIBuilder enable(@Nonnull String selector) {
        return disabled(selector, false);
    }

    /**
     * Sets the value property (for inputs, checkboxes, sliders).
     */
    @Nonnull
    public UIBuilder value(@Nonnull String selector, @Nonnull String value) {
        return set(selector + ".Value", value);
    }

    /**
     * Sets the value property (for checkboxes).
     */
    @Nonnull
    public UIBuilder value(@Nonnull String selector, boolean value) {
        return set(selector + ".Value", value);
    }

    /**
     * Sets the value property (for sliders, progress bars).
     */
    @Nonnull
    public UIBuilder value(@Nonnull String selector, float value) {
        return set(selector + ".Value", value);
    }

    /**
     * Sets anchor properties for dynamic sizing.
     */
    @Nonnull
    public UIBuilder anchor(@Nonnull String selector, @Nonnull String anchorValue) {
        return set(selector + ".Anchor", anchorValue);
    }

    /**
     * Sets element width.
     */
    @Nonnull
    public UIBuilder width(@Nonnull String selector, int width) {
        return set(selector + ".Anchor.Width", width);
    }

    /**
     * Sets element height.
     */
    @Nonnull
    public UIBuilder height(@Nonnull String selector, int height) {
        return set(selector + ".Anchor.Height", height);
    }

    /**
     * Sets element size (width and height).
     */
    @Nonnull
    public UIBuilder size(@Nonnull String selector, int width, int height) {
        return width(selector, width).height(selector, height);
    }

    // ========== Text Styling ==========

    /**
     * Sets text color of a label.
     */
    @Nonnull
    public UIBuilder textColor(@Nonnull String selector, @Nonnull String color) {
        return set(selector + ".Style.TextColor", color);
    }

    /**
     * Sets font size of a label.
     */
    @Nonnull
    public UIBuilder fontSize(@Nonnull String selector, int size) {
        return set(selector + ".Style.FontSize", size);
    }

    /**
     * Sets placeholder text for TextField.
     */
    @Nonnull
    public UIBuilder placeholder(@Nonnull String selector, @Nonnull String text) {
        return set(selector + ".PlaceholderText", text);
    }

    // ========== Input Controls ==========

    /**
     * Sets the value of a NumberField.
     */
    @Nonnull
    public UIBuilder value(@Nonnull String selector, int value) {
        return set(selector + ".Value", value);
    }

    /**
     * Sets min value for NumberField or Slider.
     */
    @Nonnull
    public UIBuilder min(@Nonnull String selector, float min) {
        return set(selector + ".Min", min);
    }

    /**
     * Sets max value for NumberField or Slider.
     */
    @Nonnull
    public UIBuilder max(@Nonnull String selector, float max) {
        return set(selector + ".Max", max);
    }

    /**
     * Sets min and max range for NumberField or Slider.
     */
    @Nonnull
    public UIBuilder range(@Nonnull String selector, float min, float max) {
        return min(selector, min).max(selector, max);
    }

    /**
     * Sets checkbox to checked state.
     */
    @Nonnull
    public UIBuilder checked(@Nonnull String selector, boolean checked) {
        return value(selector, checked);
    }

    /**
     * Checks a checkbox.
     */
    @Nonnull
    public UIBuilder check(@Nonnull String selector) {
        return checked(selector, true);
    }

    /**
     * Unchecks a checkbox.
     */
    @Nonnull
    public UIBuilder uncheck(@Nonnull String selector) {
        return checked(selector, false);
    }

    // ========== Conditional Helpers ==========

    /**
     * Enables an element if condition is true.
     */
    @Nonnull
    public UIBuilder enableIf(@Nonnull String selector, boolean condition) {
        return disabled(selector, !condition);
    }

    /**
     * Disables an element if condition is true.
     */
    @Nonnull
    public UIBuilder disableIf(@Nonnull String selector, boolean condition) {
        return disabled(selector, condition);
    }

    // ========== Layout Helpers ==========

    /**
     * Sets the flex weight for layout.
     */
    @Nonnull
    public UIBuilder flexWeight(@Nonnull String selector, float weight) {
        return set(selector + ".FlexWeight", weight);
    }

    /**
     * Sets opacity of an element (0.0 to 1.0).
     */
    @Nonnull
    public UIBuilder opacity(@Nonnull String selector, float opacity) {
        return set(selector + ".Opacity", opacity);
    }

    // ========== Container Helpers ==========

    /**
     * Sets the title text for a @Container's #Title element.
     * Use with pages that use $C.@Container template.
     */
    @Nonnull
    public UIBuilder title(@Nonnull String titleText) {
        return text("#Title #PanelTitle", titleText);
    }

    /**
     * Sets the title text for a @Title element within a container.
     * For $C.@Title templates.
     */
    @Nonnull
    public UIBuilder pageTitle(@Nonnull String selector, @Nonnull String titleText) {
        return text(selector + " Label", titleText);
    }

    // ========== Convenience Aliases ==========

    /**
     * Alias for text() - sets label content.
     */
    @Nonnull
    public UIBuilder label(@Nonnull String selector, @Nonnull String text) {
        return text(selector, text);
    }

    /**
     * Sets button text (for TextButton elements).
     */
    @Nonnull
    public UIBuilder buttonText(@Nonnull String selector, @Nonnull String text) {
        return text(selector, text);
    }

    // ========== Event Bindings ==========

    /**
     * Binds a click/activate event to a button.
     */
    @Nonnull
    public UIBuilder onClick(@Nonnull String selector, @Nonnull String action) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selector : selector;
        events.addEventBinding(CustomUIEventBindingType.Activating, fullSelector, EventData.of("Button", action), false);
        return this;
    }

    /**
     * Binds a value change event to an input field.
     */
    @Nonnull
    public UIBuilder onInput(@Nonnull String selector, @Nonnull String dataKey) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selector : selector;
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, fullSelector, EventData.of(dataKey, fullSelector + ".Value"), false);
        return this;
    }

    /**
     * Binds a custom event.
     */
    @Nonnull
    public UIBuilder onEvent(@Nonnull CustomUIEventBindingType type, @Nonnull String selector, @Nonnull String key, @Nonnull String value) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selector : selector;
        events.addEventBinding(type, fullSelector, EventData.of(key, value), false);
        return this;
    }

    /**
     * Binds a checkbox value change event.
     */
    @Nonnull
    public UIBuilder onCheck(@Nonnull String selector, @Nonnull String dataKey) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selector : selector;
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, fullSelector, EventData.of(dataKey, fullSelector + ".Value"), false);
        return this;
    }

    /**
     * Binds a slider value change event.
     */
    @Nonnull
    public UIBuilder onSlide(@Nonnull String selector, @Nonnull String dataKey) {
        return onCheck(selector, dataKey);
    }

    /**
     * Binds a dropdown selection change event.
     */
    @Nonnull
    public UIBuilder onSelect(@Nonnull String selector, @Nonnull String dataKey) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selector : selector;
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, fullSelector, EventData.of(dataKey, fullSelector + ".Value"), false);
        return this;
    }

    /**
     * Sets dropdown entries from a list of display/value pairs.
     * @param selector The dropdown selector
     * @param entries List of entries where each entry is [displayName, value]
     */
    @Nonnull
    public UIBuilder dropdownEntries(@Nonnull String selector, @Nonnull List<String[]> entries) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selector : selector;
        List<DropdownEntryInfo> dropdownEntries = entries.stream()
                .map(e -> new DropdownEntryInfo(LocalizableString.fromString(e[0]), e.length > 1 ? e[1] : e[0]))
                .toList();
        commands.set(fullSelector + ".Entries", dropdownEntries);
        return this;
    }

    /**
     * Sets dropdown entries from simple string values (display = value).
     * @param selector The dropdown selector
     * @param values List of values (used as both display and value)
     */
    @Nonnull
    public UIBuilder dropdownOptions(@Nonnull String selector, @Nonnull String... values) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selector : selector;
        List<DropdownEntryInfo> dropdownEntries = java.util.Arrays.stream(values)
                .map(v -> new DropdownEntryInfo(LocalizableString.fromString(v), v))
                .toList();
        commands.set(fullSelector + ".Entries", dropdownEntries);
        return this;
    }

    /**
     * Sets the selected value for a dropdown.
     */
    @Nonnull
    public UIBuilder selected(@Nonnull String selector, @Nonnull String value) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selector : selector;
        commands.set(fullSelector + ".Value", value);
        return this;
    }

    /**
     * Binds a mouse enter (hover) event.
     */
    @Nonnull
    public UIBuilder onHover(@Nonnull String selector, @Nonnull String action) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selector : selector;
        events.addEventBinding(CustomUIEventBindingType.MouseEntered, fullSelector, EventData.of("Hover", action), false);
        return this;
    }

    /**
     * Binds a mouse exit (hover leave) event.
     */
    @Nonnull
    public UIBuilder onHoverLeave(@Nonnull String selector, @Nonnull String action) {
        String fullSelector = currentPrefix != null ? currentPrefix + " " + selector : selector;
        events.addEventBinding(CustomUIEventBindingType.MouseExited, fullSelector, EventData.of("HoverLeave", action), false);
        return this;
    }

    // ========== List Building ==========

    /**
     * Builds a list of items with automatic indexing.
     *
     * @param containerSelector The parent container selector (e.g., "#ItemList")
     * @param containerGroupId The ID for the generated group (e.g., "ItemCards")
     * @param items The items to render
     * @param entryTemplate Path to the entry template
     * @param builder Consumer that builds each row
     */
    @Nonnull
    public <T> UIBuilder list(
            @Nonnull String containerSelector,
            @Nonnull String containerGroupId,
            @Nonnull Collection<T> items,
            @Nonnull String entryTemplate,
            @Nonnull BiConsumer<T, UIRowBuilder> builder
    ) {
        // Clear the container first (it exists in the .ui file)
        clear(containerSelector);

        // Build each row directly into the container
        int index = 0;
        for (T item : items) {
            append(containerSelector, entryTemplate);
            String rowPrefix = containerSelector + "[" + index + "]";
            UIRowBuilder rowBuilder = new UIRowBuilder(this, rowPrefix, index);
            builder.accept(item, rowBuilder);
            index++;
        }

        return this;
    }

    /**
     * Simplified list builder.
     */
    @Nonnull
    public <T> UIBuilder list(
            @Nonnull String containerSelector,
            @Nonnull Collection<T> items,
            @Nonnull String entryTemplate,
            @Nonnull BiConsumer<T, UIRowBuilder> builder
    ) {
        // Clear the container first (it exists in the .ui file)
        clear(containerSelector);

        // Build each row directly into the container
        int index = 0;
        for (T item : items) {
            append(containerSelector, entryTemplate);
            String rowPrefix = containerSelector + "[" + index + "]";
            UIRowBuilder rowBuilder = new UIRowBuilder(this, rowPrefix, index);
            builder.accept(item, rowBuilder);
            index++;
        }

        return this;
    }

    /**
     * Sets a prefix for all subsequent selectors.
     */
    @Nonnull
    public UIBuilder withPrefix(@Nonnull String prefix) {
        this.currentPrefix = prefix;
        return this;
    }

    /**
     * Clears the current prefix.
     */
    @Nonnull
    public UIBuilder clearPrefix() {
        this.currentPrefix = null;
        return this;
    }

    /**
     * Gets the underlying command builder.
     */
    @Nonnull
    public UICommandBuilder getCommands() {
        return commands;
    }

    /**
     * Gets the underlying event builder.
     */
    @Nonnull
    public UIEventBuilder getEvents() {
        return events;
    }

    /**
     * Builder for individual list rows.
     */
    public static class UIRowBuilder {
        private final UIBuilder parent;
        private final String prefix;
        private final int index;

        UIRowBuilder(@Nonnull UIBuilder parent, @Nonnull String prefix, int index) {
            this.parent = parent;
            this.prefix = prefix;
            this.index = index;
        }

        /**
         * Sets an element property within this row (String value).
         */
        @Nonnull
        public UIRowBuilder set(@Nonnull String selectorAndProperty, @Nonnull String value) {
            parent.commands.set(prefix + " " + selectorAndProperty, value);
            return this;
        }

        /**
         * Sets an element property within this row (float value).
         */
        @Nonnull
        public UIRowBuilder set(@Nonnull String selectorAndProperty, float value) {
            parent.commands.set(prefix + " " + selectorAndProperty, value);
            return this;
        }

        /**
         * Sets an element property within this row (int value).
         */
        @Nonnull
        public UIRowBuilder set(@Nonnull String selectorAndProperty, int value) {
            parent.commands.set(prefix + " " + selectorAndProperty, value);
            return this;
        }

        /**
         * Sets an element property within this row (boolean value).
         */
        @Nonnull
        public UIRowBuilder set(@Nonnull String selectorAndProperty, boolean value) {
            parent.commands.set(prefix + " " + selectorAndProperty, value);
            return this;
        }

        /**
         * Sets text content of an element.
         */
        @Nonnull
        public UIRowBuilder text(@Nonnull String selector, @Nonnull String text) {
            return set(selector + ".Text", text);
        }

        /**
         * Sets background color of an element.
         */
        @Nonnull
        public UIRowBuilder background(@Nonnull String selector, @Nonnull String color) {
            return set(selector + ".Background", color);
        }

        /**
         * Sets the value of a ProgressBar element (0.0 to 1.0).
         */
        @Nonnull
        public UIRowBuilder progress(@Nonnull String selector, float value) {
            return set(selector + ".Value", value);
        }

        /**
         * Sets visibility of an element.
         */
        @Nonnull
        public UIRowBuilder visible(@Nonnull String selector, boolean visible) {
            return set(selector + ".Visible", visible);
        }

        /**
         * Shows an element.
         */
        @Nonnull
        public UIRowBuilder show(@Nonnull String selector) {
            return visible(selector, true);
        }

        /**
         * Hides an element.
         */
        @Nonnull
        public UIRowBuilder hide(@Nonnull String selector) {
            return visible(selector, false);
        }

        /**
         * Conditionally shows an element.
         */
        @Nonnull
        public UIRowBuilder showIf(@Nonnull String selector, boolean condition) {
            return visible(selector, condition);
        }

        /**
         * Conditionally hides an element.
         */
        @Nonnull
        public UIRowBuilder hideIf(@Nonnull String selector, boolean condition) {
            return visible(selector, !condition);
        }

        /**
         * Disables an element.
         */
        @Nonnull
        public UIRowBuilder disabled(@Nonnull String selector, boolean disabled) {
            return set(selector + ".Disabled", disabled);
        }

        /**
         * Disables an element.
         */
        @Nonnull
        public UIRowBuilder disable(@Nonnull String selector) {
            return disabled(selector, true);
        }

        /**
         * Enables an element.
         */
        @Nonnull
        public UIRowBuilder enable(@Nonnull String selector) {
            return disabled(selector, false);
        }

        /**
         * Sets the value of an input/checkbox/slider.
         */
        @Nonnull
        public UIRowBuilder value(@Nonnull String selector, @Nonnull String value) {
            return set(selector + ".Value", value);
        }

        /**
         * Sets the value of a checkbox.
         */
        @Nonnull
        public UIRowBuilder value(@Nonnull String selector, boolean value) {
            return set(selector + ".Value", value);
        }

        /**
         * Sets text color.
         */
        @Nonnull
        public UIRowBuilder textColor(@Nonnull String selector, @Nonnull String color) {
            return set(selector + ".Style.TextColor", color);
        }

        /**
         * Sets opacity.
         */
        @Nonnull
        public UIRowBuilder opacity(@Nonnull String selector, float opacity) {
            return set(selector + ".Opacity", opacity);
        }

        /**
         * Binds a click event with the action containing this row's index.
         */
        @Nonnull
        public UIRowBuilder onClick(@Nonnull String selector, @Nonnull String action) {
            parent.events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    prefix + " " + selector,
                    EventData.of("Button", action),
                    false
            );
            return this;
        }

        /**
         * Binds a click event with action:index format.
         */
        @Nonnull
        public UIRowBuilder onClickIndexed(@Nonnull String selector, @Nonnull String actionPrefix) {
            return onClick(selector, actionPrefix + ":" + index);
        }

        /**
         * Binds a custom event.
         */
        @Nonnull
        public UIRowBuilder onEvent(@Nonnull CustomUIEventBindingType type, @Nonnull String selector, @Nonnull String key, @Nonnull String value) {
            parent.events.addEventBinding(type, prefix + " " + selector, EventData.of(key, value), false);
            return this;
        }

        /**
         * Gets the row index.
         */
        public int getIndex() {
            return index;
        }

        /**
         * Gets the row prefix selector.
         */
        @Nonnull
        public String getPrefix() {
            return prefix;
        }
    }
}
