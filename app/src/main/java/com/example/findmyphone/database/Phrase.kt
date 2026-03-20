package com.example.findmyphone.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phrases")
data class Phrase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val template: FloatArray,
    val isActive: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Phrase

        if (id != other.id) return false
        if (name != other.name) return false
        if (!template.contentEquals(other.template)) return false
        if (isActive != other.isActive) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + template.contentHashCode()
        result = 31 * result + isActive.hashCode()
        return result
    }
}
