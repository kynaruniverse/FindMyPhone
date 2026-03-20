package com.example.findmyphone.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PhraseDao {
    @Insert
    suspend fun insert(phrase: Phrase)

    @Update
    suspend fun update(phrase: Phrase)

    @Query("SELECT * FROM phrases WHERE isActive = 1")
    suspend fun getAllActive(): List<Phrase>

    @Query("SELECT * FROM phrases")
    suspend fun getAll(): List<Phrase>

    @Query("DELETE FROM phrases WHERE id = :id")
    suspend fun deleteById(id: Int)
}
