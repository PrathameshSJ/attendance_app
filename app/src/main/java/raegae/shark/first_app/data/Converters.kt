package raegae.shark.first_app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        return value.split(",").map { it.trim() }
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun fromMapString(value: String): Map<String, String> {
        if (value.isEmpty()) return emptyMap()
        return value.split(",").associate {
            val parts = it.split(":")
            parts[0] to parts[1]
        }
    }

    @TypeConverter
    fun toMapString(map: Map<String, String>): String {
        return map.entries.joinToString(",") { "${it.key}:${it.value}" }
    }
}
