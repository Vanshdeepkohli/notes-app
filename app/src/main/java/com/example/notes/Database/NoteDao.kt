package com.example.notes.Database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao()
interface NoteDao {

    @Query("SELECT * FROM NOTE ORDER BY date desc")
    fun getAllNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM NOTE ORDER BY date asc")
    fun getAllNotesAsc(): LiveData<List<Note>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNote(note: Note)

    @Delete()
    suspend fun deleteNote(note: Note)

    @Update()
    suspend fun updateNote(note: Note)

    @Query("UPDATE Note SET isSelected = :selected WHERE id =:id")
    suspend fun updateIsSelected(id: Int, selected: Int)

    @Query("DELETE FROM Note WHERE id = :id")
    suspend fun deleteAt(id: Int)
}