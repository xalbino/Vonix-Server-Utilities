package network.vonix.serverutilities.kits;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure kit-definition parsing and group normalisation.
 * Missing, blank, or non-text {@code group} values default to the kit name
 * so existing {@code kits.json} files keep independent per-kit cooldowns.
 */
public final class KitGroupRules {
    private KitGroupRules() {}

    public record ParsedItem(String itemId, int count) {}

    public record ParsedKit(
            String name,
            String group,
            int cooldownSeconds,
            boolean oneTime,
            List<ParsedItem> items) {}

    /** Trim + lower-case. Blank or null names are rejected ({@code null}). */
    public static String normalizeName(String raw) {
        if (raw == null) return null;
        String name = raw.trim().toLowerCase(Locale.ROOT);
        return name.isEmpty() ? null : name;
    }

    /**
     * Missing/blank/whitespace groups default to the kit name.
     * The result is always trimmed and lower-cased.
     */
    public static String normalizeGroup(String rawGroup, String kitName) {
        String name = normalizeName(kitName);
        if (name == null) return null;
        if (rawGroup == null) return name;
        String group = rawGroup.trim().toLowerCase(Locale.ROOT);
        return group.isEmpty() ? name : group;
    }

    /**
     * Read {@code group} from a kit object. Non-string JSON (objects, arrays,
     * null) is treated as missing and defaults to the kit name. Primitive
     * non-strings (numbers, booleans) use their deterministic JSON string
     * form, then the same blank-defaulting rules.
     */
    public static String readGroupField(JsonObject kit, String kitName) {
        if (kit == null || !kit.has("group") || kit.get("group").isJsonNull()) {
            return normalizeGroup(null, kitName);
        }
        JsonElement el = kit.get("group");
        if (!el.isJsonPrimitive()) {
            return normalizeGroup(null, kitName);
        }
        JsonPrimitive primitive = el.getAsJsonPrimitive();
        return normalizeGroup(primitive.getAsString(), kitName);
    }

    public static ParsedKit parseKitObject(JsonObject kit) {
        if (kit == null) return null;
        String rawName = null;
        if (kit.has("name") && kit.get("name").isJsonPrimitive()) {
            rawName = kit.get("name").getAsString();
        }
        String name = normalizeName(rawName);
        if (name == null) return null;

        String group = readGroupField(kit, name);
        int cooldown = 3600;
        if (kit.has("cooldown_seconds") && kit.get("cooldown_seconds").isJsonPrimitive()) {
            try {
                cooldown = kit.get("cooldown_seconds").getAsInt();
            } catch (Exception ignore) {
                cooldown = 3600;
            }
        }
        boolean oneTime = false;
        if (kit.has("one_time") && kit.get("one_time").isJsonPrimitive()) {
            try {
                oneTime = kit.get("one_time").getAsBoolean();
            } catch (Exception ignore) {
                oneTime = false;
            }
        }

        List<ParsedItem> items = new ArrayList<>();
        if (kit.has("items") && kit.get("items").isJsonArray()) {
            for (JsonElement itemEl : kit.getAsJsonArray("items")) {
                if (!itemEl.isJsonObject()) continue;
                JsonObject io = itemEl.getAsJsonObject();
                if (!io.has("item") || !io.get("item").isJsonPrimitive()) continue;
                String itemId = io.get("item").getAsString();
                if (itemId == null || itemId.isBlank()) continue;
                int count = 1;
                if (io.has("count") && io.get("count").isJsonPrimitive()) {
                    try {
                        count = io.get("count").getAsInt();
                    } catch (Exception ignore) {
                        count = 1;
                    }
                }
                items.add(new ParsedItem(itemId, count));
            }
        }
        return new ParsedKit(name, group, cooldown, oneTime, List.copyOf(items));
    }

    public static List<ParsedKit> parseKitsJson(String json) {
        List<ParsedKit> out = new ArrayList<>();
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) return List.of();
        JsonObject obj = root.getAsJsonObject();
        JsonArray arr = obj.has("kits") && obj.get("kits").isJsonArray()
                ? obj.getAsJsonArray("kits") : new JsonArray();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            ParsedKit parsed = parseKitObject(el.getAsJsonObject());
            if (parsed != null) out.add(parsed);
        }
        return List.copyOf(out);
    }
}
