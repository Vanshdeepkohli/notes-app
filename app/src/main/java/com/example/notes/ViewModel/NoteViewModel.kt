package com.example.notes.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notes.Database.Note
import com.example.notes.Repository.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(private val userRepository: NoteRepository) : ViewModel() {

    fun getNotes(): LiveData<List<Note>> = userRepository.getNotes()

    fun getNotesAsc(): LiveData<List<Note>> = userRepository.getNotesAsc()

    fun insertNote(note: Note) {
        viewModelScope.launch {
            userRepository.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            userRepository.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            userRepository.deleteNote(note)
        }
    }

    fun updateIsSelected(id: Int, selected: Int) {
        viewModelScope.launch {
            userRepository.updateIsSelected(id, selected)
        }
    }

    fun deleteAt(id: Int) {
        viewModelScope.launch {
            userRepository.deleteAt(id)
        }
    }


}