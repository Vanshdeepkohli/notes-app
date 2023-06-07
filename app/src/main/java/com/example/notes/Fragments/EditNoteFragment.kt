package com.example.notes.Fragments

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.notes.Database.Note
import com.example.notes.Database.NoteDao
import com.example.notes.Database.NoteDatabase
import com.example.notes.R
import com.example.notes.Repository.NoteRepository
import com.example.notes.ViewModel.NoteViewModel
import com.example.notes.databinding.FragmentEditNoteBinding
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EditNoteFragment : Fragment() {

    private var _binding : FragmentEditNoteBinding? = null
    private  val binding get() = _binding!!

    private lateinit var userDao : NoteDao
    private lateinit var repository : NoteRepository
    private lateinit var viewModel : NoteViewModel

    val currentNote by navArgs<EditNoteFragmentArgs>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentEditNoteBinding.inflate(inflater,container,false)

        userDao = NoteDatabase.getDatabase(this.requireContext()).getDao()
        repository = NoteRepository(userDao)
        viewModel = NoteViewModel(repository)



        binding.title.setText(currentNote.currentNote.title)
        binding.content.setText(currentNote.currentNote.content)
        binding.date.text = "Last updated : ${currentNote.currentNote.date}"




        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_editNoteFragment_to_viewAllNotesFragment)
            _binding = null
        }

        binding.save.setOnClickListener {
            updateNote(currentNote.currentNote)
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateNote(note : Note){

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val currentDate = LocalDateTime.now().format(formatter)

        viewModel.updateNote(Note(currentNote.currentNote.id,binding.title.text.toString(),binding.content.text.toString(),currentDate))

        Toast.makeText(context,"Updated!", Toast.LENGTH_SHORT).show()

        findNavController().navigate(R.id.action_editNoteFragment_to_viewAllNotesFragment)
        _binding = null
    }

}