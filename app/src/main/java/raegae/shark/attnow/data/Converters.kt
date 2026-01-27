package raegae.shark.attnow.data

import androidx.room.TypeConverter
import org.json.JSONObject
import org.json.JSONArray

class Converters {

    /* ---------- Map<String, String> ---------- */

    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String {
        if (map == null) return "{}"
        return JSONObject(map).toString()
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        val json = JSONObject(value)
        val result = mutableMapOf<String, String>()
        json.keys().forEach { key ->
            result[key] = json.getString(key)
        }
        return result
    }

    /* ---------- List<String> ---------- */

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list == null) return "[]"
        return JSONArray(list).toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { i -> array.getString(i) }
    }
}
