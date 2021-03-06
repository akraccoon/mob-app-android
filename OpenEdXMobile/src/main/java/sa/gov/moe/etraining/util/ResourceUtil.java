package sa.gov.moe.etraining.util;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;

import com.squareup.phrase.Phrase;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import sa.gov.moe.etraining.logger.Logger;


public class ResourceUtil {
    private static final Logger logger = new Logger(ResourceUtil.class);
    public static final String QuantityHolder = "quantity";

    public static CharSequence getFormattedString(@NonNull Resources resources, @StringRes int resourceId, @NonNull String key, @Nullable CharSequence value) {
        return getFormattedString(resources.getString(resourceId), Collections.singletonMap(key, value));
    }

    public static CharSequence getFormattedString(@NonNull Resources resources, @StringRes int resourceId, @NonNull Map<String, CharSequence> keyValMap) {
        return getFormattedString(resources.getString(resourceId), keyValMap);
    }

    public static CharSequence getFormattedString(@NonNull String pattern, @NonNull String key, @Nullable CharSequence value) {
        return getFormattedString(pattern, Collections.singletonMap(key, value));
    }

    public static CharSequence getFormattedString(@NonNull String pattern, @NonNull Map<String, ? extends CharSequence> keyValMap) {
        Phrase resourceString = Phrase.from(pattern);
        Set<String> keys = keyValMap.keySet();
        for (String key : keys) {
            CharSequence val = keyValMap.get(key);
            if (val == null) {
                logger.warn(String.format("Value for key %s is null", key));
                resourceString.put(key, "");
            } else {
                resourceString.put(key, val);
            }
        }
        return resourceString.format();
    }

    public static CharSequence getFormattedStringForQuantity(@NonNull Resources resources, @PluralsRes int resourceId, int quantity) {
        return getFormattedStringForQuantity(resources, resourceId, QuantityHolder, quantity);
    }

    public static CharSequence getFormattedStringForQuantity(@NonNull Resources resources, @PluralsRes int resourceId, @NonNull String key, int quantity) {
        return getFormattedStringForQuantity(resources, resourceId, quantity, Collections.singletonMap(key, quantity + ""));
    }

    public static CharSequence getFormattedStringForQuantity(@NonNull Resources resources,
                                                             @PluralsRes int resourceId, int quantity,
                                                             @NonNull Map<String, String> keyValMap) {
        String template = resources.getQuantityString(resourceId, quantity);
        return getFormattedString(template, keyValMap);
    }
}
