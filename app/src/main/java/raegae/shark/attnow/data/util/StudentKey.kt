package raegae.shark.attnow.data.util

import java.io.Serializable

data class StudentKey(
    val name: String,
    val subject: String
) : Serializable {

    /** Used for DataStore / persistence */
    fun serialize(): String = "$name||$subject"

    companion object {

        fun fromString(raw: String): StudentKey? {
            val parts = raw.split("||")
            if (parts.size != 2) return null
            return StudentKey(
                name = parts[0],
                subject = parts[1]
            )
        }
    }
}
