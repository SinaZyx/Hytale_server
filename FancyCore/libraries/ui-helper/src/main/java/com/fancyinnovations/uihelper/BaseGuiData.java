package com.fancyinnovations.uihelper;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nullable;

/**
 * Base data class for GUI events.
 * Contains common fields used across most GUIs.
 *
 * Extend this class to add custom fields for your GUI.
 *
 * Example:
 * <pre>
 * public class MyGuiData extends BaseGuiData {
 *     public static final BuilderCodec&gt;MyGuiData> CODEC = BaseGuiData.codec(MyGuiData.class, MyGuiData::new)
 *             .addField(new KeyedCodec&gt;>("CustomField", Codec.STRING), (d, v) -> d.customField = v, d -> d.customField)
 *             .build();
 *
 *     public String customField;
 * }
 * </pre>
 */
public class BaseGuiData {

    public static final String KEY_BUTTON = "Button";
    public static final String KEY_INPUT = "@Input";
    public static final String KEY_INDEX = "Index";
    /**
     * Default codec for BaseGuiData (use when no custom fields needed).
     */
    public static final BuilderCodec<BaseGuiData> CODEC = codec(BaseGuiData.class, BaseGuiData::new).build();
    /**
     * Button action string (e.g., "Save", "Edit:itemName", "Delete:5").
     */
    @Nullable
    public String button;
    /**
     * Input field value.
     */
    @Nullable
    public String input;
    /**
     * Index value for list operations.
     */
    public int index = -1;

    public BaseGuiData() {}

    /**
     * Creates the base codec builder that includes common fields.
     */
    public static <T extends BaseGuiData> BuilderCodec.Builder<T> codec(Class<T> clazz, java.util.function.Supplier<T> constructor) {
        return BuilderCodec.<T>builder(clazz, constructor)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, v) -> d.button = v, d -> d.button)
                .addField(new KeyedCodec<>(KEY_INPUT, Codec.STRING), (d, v) -> d.input = v, d -> d.input)
                .addField(new KeyedCodec<>(KEY_INDEX, Codec.INTEGER), (d, v) -> d.index = v, d -> d.index);
    }

    /**
     * Parses the button field as a UIAction.
     */
    public UIAction action() {
        return UIAction.parse(button);
    }

    /**
     * Checks if this is a button event.
     */
    public boolean hasButton() {
        return button != null && !button.isEmpty();
    }

    /**
     * Checks if this is an input event.
     */
    public boolean hasInput() {
        return input != null;
    }
}
