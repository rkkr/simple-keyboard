package rkr.simplekeyboard.inputmethod.latin.utils;

import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.Arrays;

import rkr.simplekeyboard.inputmethod.R;

import static rkr.simplekeyboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.IS_ADDITIONAL_SUBTYPE;
import static rkr.simplekeyboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.KEYBOARD_LAYOUT_SET;
import static rkr.simplekeyboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME;
import static rkr.simplekeyboard.inputmethod.latin.common.Constants.Subtype.KEYBOARD_MODE;

//TODO: delete - moved from AdditionalSubtypeUtils to keep for testing
public class InputMethodSubtypeUtils {
    private static final String TAG = InputMethodSubtypeUtils.class.getSimpleName();

    private InputMethodSubtypeUtils() {
        // This utility class is not publicly instantiable.
    }

    private static InputMethodSubtype createAdditionalSubtypeInternal(
            final String localeString, final String keyboardLayoutSetName) {
        final int nameId = SubtypeLocaleUtils.getSubtypeNameId(localeString, keyboardLayoutSetName);
        final String platformVersionDependentExtraValues = getPlatformVersionDependentExtraValue(
                localeString, keyboardLayoutSetName);
        final int platformVersionIndependentSubtypeId =
                getPlatformVersionIndependentSubtypeId(localeString, keyboardLayoutSetName);
        InputMethodSubtype.InputMethodSubtypeBuilder builder = new InputMethodSubtype.InputMethodSubtypeBuilder();

        builder.setSubtypeNameResId(nameId)
                .setSubtypeIconResId(R.drawable.ic_ime_switcher_dark)
                .setSubtypeLocale(localeString)
                .setSubtypeMode(KEYBOARD_MODE)
                .setSubtypeExtraValue(platformVersionDependentExtraValues)
                .setOverridesImplicitlyEnabledSubtype(false)
                .setIsAuxiliary(false)
                .setSubtypeId(platformVersionIndependentSubtypeId);
        return builder.build();
    }

    public static InputMethodSubtype createAdditionalSubtype(
            final String localeString, final String keyboardLayoutSetName) {
        return createAdditionalSubtypeInternal(localeString, keyboardLayoutSetName);
    }

    /**
     * Returns the extra value that is optimized for the running OS.
     * <p>
     * Historically the extra value has been used as the last resort to annotate various kinds of
     * attributes. Some of these attributes are valid only on some platform versions. Thus we cannot
     * assume that the extra values stored in a persistent storage are always valid. We need to
     * regenerate the extra value on the fly instead.
     * </p>
     * @param keyboardLayoutSetName the keyboard layout set name (e.g., "dvorak").
     * @return extra value that is optimized for the running OS.
     * @see #getPlatformVersionIndependentSubtypeId(String, String)
     */
    private static String getPlatformVersionDependentExtraValue(final String localeString, final String keyboardLayoutSetName) {
        final ArrayList<String> extraValueItems = new ArrayList<>();
        extraValueItems.add(KEYBOARD_LAYOUT_SET + "=" + keyboardLayoutSetName);
        if (SubtypeLocaleUtils.isExceptionalLocale(localeString)) {
            extraValueItems.add(UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME + "=" +
                    SubtypeLocaleUtils.getKeyboardLayoutSetDisplayName(keyboardLayoutSetName));
        }
        extraValueItems.add(IS_ADDITIONAL_SUBTYPE);
        return TextUtils.join(",", extraValueItems);
    }

    /**
     * Returns the subtype ID that is supposed to be compatible between different version of OSes.
     * <p>
     * From the compatibility point of view, it is important to keep subtype id predictable and
     * stable between different OSes. For this purpose, the calculation code in this method is
     * carefully chosen and then fixed. Treat the following code as no more or less than a
     * hash function. Each component to be hashed can be different from the corresponding value
     * that is used to instantiate {@link InputMethodSubtype} actually.
     * For example, you don't need to update <code>compatibilityExtraValueItems</code> in this
     * method even when we need to add some new extra values for the actual instance of
     * {@link InputMethodSubtype}.
     * </p>
     * @param localeString the locale string (e.g., "en_US").
     * @param keyboardLayoutSetName the keyboard layout set name (e.g., "dvorak").
     * @return a platform-version independent subtype ID.
     */
    private static int getPlatformVersionIndependentSubtypeId(final String localeString,
                                                              final String keyboardLayoutSetName) {
        // For compatibility reasons, we concatenate the extra values in the following order.
        // - KeyboardLayoutSet
        // - AsciiCapable
        // - UntranslatableReplacementStringInSubtypeName
        // - EmojiCapable
        // - isAdditionalSubtype
        final ArrayList<String> compatibilityExtraValueItems = new ArrayList<>();
        compatibilityExtraValueItems.add(KEYBOARD_LAYOUT_SET + "=" + keyboardLayoutSetName);
        if (SubtypeLocaleUtils.isExceptionalLocale(localeString)) {
            compatibilityExtraValueItems.add(UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME + "=" +
                    SubtypeLocaleUtils.getKeyboardLayoutSetDisplayName(keyboardLayoutSetName));
        }
        compatibilityExtraValueItems.add(IS_ADDITIONAL_SUBTYPE);
        final String compatibilityExtraValues = TextUtils.join(",", compatibilityExtraValueItems);
        return Arrays.hashCode(new Object[] {
                localeString,
                KEYBOARD_MODE,
                compatibilityExtraValues,
                false /* isAuxiliary */,
                false /* overrideImplicitlyEnabledSubtype */ });
    }
}
