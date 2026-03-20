package com.example.findmyphone.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(entities = [Phrase::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao
}

class Converters {
    @TypeConverter
    fun fromFloatArray(value: FloatArray): ByteArray {
        val intArray = value.map { it.toRawBits() }.toIntArray()
        val byteArray = ByteArray(intArray.size * 4)
        for (i in intArray.indices) {
            val intVal = intArray[i]
            byteArray[i * 4] = (intVal shr 24).toByte()
            byteArray[i * 4 + 1] = (intVal shr 16).toByte()
            byteArray[i * 4 + 2] = (intVal shr 8).toByte()
            byteArray[i * 4 + 3] = intVal.toByte()
        }
        return byteArray
    }

    @TypeConverter
    fun toFloatArray(value: ByteArray): FloatArray {
        val floatArray = FloatArray(value.size / 4)
        for (i in floatArray.indices) {
            val intVal = (value[i * 4].toInt() and 0xFF shl 24) or
                    (value[i * 4 + 1].toInt() and 0xFF shl 16) or
                    (value[i * 4 + 2].toInt() and 0xFF shl 8) or
                    (value[i * 4 + 3].toInt() and 0xFF)
            floatArray[i] = Float.fromBits(intVal)
        }
        return floatArray
    }
}
