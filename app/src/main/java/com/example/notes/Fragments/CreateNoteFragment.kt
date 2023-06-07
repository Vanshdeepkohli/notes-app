package com.example.notes.Fragments

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.notes.Database.Note
import com.example.notes.Database.NoteDao
import com.example.notes.Database.NoteDatabase
import com.example.notes.R
import com.example.notes.Repository.NoteRepository
import com.example.notes.ViewModel.NoteViewModel
import com.example.notes.databinding.FragmentCreateNoteBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class CreateNoteFragment : Fragment() {

    private var _binding : FragmentCreateNoteBinding? = null
    private val binding get() = _binding!!

    private lateinit var userDao : NoteDao
    private lateinit var repository : NoteRepository
    private lateinit var viewModel : NoteViewModel



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.setActionBar(null)
        // Inflate the layout for this fragment
        _binding = FragmentCreateNoteBinding.inflate(inflater,container,false)

        userDao = NoteDatabase.getDatabase(this.requireContext()).getDao()
        repository = NoteRepository(userDao)
        viewModel = NoteViewModel(repository)

        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_createNoteFragment_to_viewAllNotesFragment)
            _binding = null
        }

        binding.save.setOnClickListener {
            if(binding.title.text.toString() == "" && binding.content.text.toString() == ""){
                Toast.makeText(context,"Enter something",Toast.LENGTH_SHORT).show()
            }else{
                createNote()
            }
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNote() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val currentDate = LocalDateTime.now().format(formatter)

        viewModel.insertNote(Note(null,binding.title.text.toString(),binding.content.text.toString(),currentDate))

        Toast.makeText(context,"Saved!", Toast.LENGTH_SHORT).show()

        findNavController().navigate(R.id.action_createNoteFragment_to_viewAllNotesFragment)
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}