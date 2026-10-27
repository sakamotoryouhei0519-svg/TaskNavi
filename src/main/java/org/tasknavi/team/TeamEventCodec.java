package org.tasknavi.team;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * {@link TeamEvent} と JSON 1 行の相互変換。
 */
final class TeamEventCodec {
    /**
     * このアプリが読める一番新しい形。記録の形を変えたら（項目や Op を足したら）上げる。
     * 古い版のアプリは、自分より新しい形の行を読まずに更新を促す。
     */
    static final int FORMAT = 2;

    /**
     * 行に書く形。古い版のアプリでも読める記録は古い形のまま書き、読めない記録（コメント）だけを上げる。
     * 全部を上げると、更新していない人がタスクの変更まで読めなくなるため。
     */
    static int formatOf(TeamEvent event) {
        return event.op() == TeamEvent.Op.COMMENT ? 2 : 1;
    }

    /** 自分より新しい版のアプリで書かれた行。 */
    static final class NewerFormatException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        NewerFormatException(int format) {
            super("新しい版のアプリで書かれた記録です: v=" + format);
        }
    }

    private TeamEventCodec() {
    }

    static String toLine(TeamEvent event) {
        JsonObject json = new JsonObject();
        json.addProperty("v", formatOf(event));
        json.addProperty("id", event.eventId());
        json.addProperty("task", event.taskId());
        json.addProperty("op", event.op().name());
        json.addProperty("clock", event.clock());
        json.addProperty("device", event.deviceId());
        json.addProperty("author", event.author());
        json.addProperty("at", event.at().toString());
        if (!event.values().isEmpty()) {
            json.add("values", toJson(event.values()));
        }
        if (!event.base().isEmpty()) {
            json.add("base", toJson(event.base()));
        }
        if (event.baseEventId() != null) {
            json.addProperty("baseEvent", event.baseEventId());
        }
        return json.toString();
    }

    /** 読めない行は IllegalArgumentException。 */
    static TeamEvent fromLine(String line) {
        try {
            JsonObject json = JsonParser.parseString(line).getAsJsonObject();
            int format = json.has("v") ? json.get("v").getAsInt() : FORMAT;
            if (format > FORMAT) {
                throw new NewerFormatException(format);
            }
            return new TeamEvent(
                    requiredString(json, "id"),
                    requiredString(json, "task"),
                    TeamEvent.Op.valueOf(requiredString(json, "op")),
                    json.get("clock").getAsLong(),
                    requiredString(json, "device"),
                    optionalString(json, "author"),
                    json.has("at") ? Instant.parse(json.get("at").getAsString()) : null,
                    fromJson(json.get("values")),
                    fromJson(json.get("base")),
                    optionalString(json, "baseEvent"));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("記録の行を読めません: " + e.getMessage(), e);
        }
    }

    private static JsonObject toJson(Map<TeamField, String> map) {
        JsonObject json = new JsonObject();
        map.forEach((field, value) -> {
            if (value == null) {
                json.add(field.key(), JsonNull.INSTANCE);
            } else {
                json.addProperty(field.key(), value);
            }
        });
        return json;
    }

    private static Map<TeamField, String> fromJson(JsonElement element) {
        Map<TeamField, String> map = new EnumMap<>(TeamField.class);
        if (element == null || !element.isJsonObject()) {
            return map;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            TeamField field = TeamField.fromKey(entry.getKey());
            if (field != null) {
                map.put(field, entry.getValue().isJsonNull() ? null : entry.getValue().getAsString());
            }
        }
        return map;
    }

    private static String requiredString(JsonObject json, String key) {
        String value = optionalString(json, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " がありません");
        }
        return value;
    }

    private static String optionalString(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }
}
