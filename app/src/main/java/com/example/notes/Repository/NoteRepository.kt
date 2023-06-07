package com.example.notes.Repository

import androidx.lifecycle.LiveData
import com.example.notes.Database.Note
import com.example.notes.Database.NoteDao

class NoteRepository(private val userDao: NoteDao) {

    fun getNotes() : LiveData<List<Note>> = userDao.getAllNotes()

    fun getNotesAsc() : LiveData<List<Note>> = userDao.getAllNotesAsc()

    suspend fun insertNote(note: Note) = userDao.insertNote(note)

    suspend fun deleteNote(note: Note) = userDao.deleteNote(note)

    suspend fun updateNote(note: Note) = userDao.updateNote(note)
}