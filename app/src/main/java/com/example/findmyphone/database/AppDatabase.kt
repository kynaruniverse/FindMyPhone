package com.example.findmyphone.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(entities = [Phrase::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao
}

class Converters {
    @TypeConverter
    fun fromFloatArray(value: FloatArray): ByteArray {
        return value.map { it.toRawBits() }.toIntArray().let { intArray ->
            intArray.foldIndexed(ByteArray(intArray.size * 4)) { index, acc, intVal ->
                val bytes = intToByteArray(intVal)
                System.arraycopy(bytes, 0, acc, index * 4, 4)
                acc
            }
        }
    }

    @TypeConverter
    fun toFloatArray(value: ByteArray): FloatArray {
        return value.chunked(4).map { chunk ->
            val intVal = byteArrayToInt(chunk.toByteArray())
            Float.fromBits(intVal)
        }.toFloatArray()
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }

    private fun byteArrayToInt(bytes: ByteArray): Int {
        return (bytes[0].toInt() and 0xFF shl 24) or
                (bytes[1].toInt() and 0xFF shl 16) or
                (bytes[2].toInt() and 0xFF shl 8) or
                (bytes[3].toInt() and 0xFF)
    }
}
