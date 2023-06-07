package com.example.notes.Fragments

import AllNotesAdapter
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notes.Database.Note
import com.example.notes.Database.NoteDao
import com.example.notes.Database.NoteDatabase
import com.example.notes.R
import com.example.notes.Repository.NoteRepository
import com.example.notes.SharedPreference.SharedPrefLiveData
import com.example.notes.ViewModel.NoteViewModel
import com.example.notes.databinding.FragmentViewAllNotesBinding

class ViewAllNotesFragment : Fragment(), MenuProvider, AllNotesAdapter.onItemClick, AllNotesAdapter.onItemLongClick, AllNotesAdapter.OnSelectionChangedListener {

    private var _binding: FragmentViewAllNotesBinding? = null
    private val binding get() = _binding!!
    lateinit var toggle: ActionBarDrawerToggle

    private lateinit var userDao: NoteDao
    private lateinit var repository: NoteRepository
    private lateinit var viewModel: NoteViewModel
    private lateinit var sharedPref: SharedPreferences
    private lateinit var sharedPrefLiveData: SharedPrefLiveData
    private lateinit var editor: SharedPreferences.Editor
    private var isGrid = true
    private var oldNotes = arrayListOf<Note>()
    private lateinit var adapter: AllNotesAdapter
    private lateinit var menu: Menu
    private val selectedNotes = mutableListOf<Note>()
    private var isSelectionMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentViewAllNotesBinding.inflate(inflater, container, false)
        adapter = AllNotesAdapter(emptyList(), this, this)
        adapter.setOnSelectionChangedListener(this)

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)

        toggle = ActionBarDrawerToggle(activity, binding.drawerLayout, R.string.open, R.string.close)
        toggle.syncState()

        userDao = NoteDatabase.getDatabase(requireContext()).getDao()
        repository = NoteRepository(userDao)
        viewModel = NoteViewModel(repository)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewModel.getNotes().observe(viewLifecycleOwner) { notes ->
            binding.totalNotes.text = "${notes.size} notes"
            adapter.setData(notes)
            oldNotes = notes as ArrayList<Note>
        }

        sharedPrefLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.rvViewAllNotes.layoutManager = GridLayoutManager(context, 2)
            } else {
                binding.rvViewAllNotes.layoutManager = LinearLayoutManager(context)
            }
        })


        binding.fabCreateNote.setOnClickListener {
            findNavController().navigate(R.id.action_viewAllNotesFragment_to_createNoteFragment)
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.login -> {
                    Toast.makeText(activity, "Login", Toast.LENGTH_SHORT).show()
                }
                R.id.sync -> {
                    Toast.makeText(activity, "Sync", Toast.LENGTH_SHORT).show()
                }
                R.id.logOut -> {
                    Toast.makeText(activity, "Logout", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }


        // date filter
        var isDown = true
        binding.filterNotes.setOnClickListener{
            if(isDown){
                binding.filterNotes.setImageResource(R.drawable.up_arrow)
                viewModel.getNotesAsc().observe(viewLifecycleOwner) { notes ->

                    binding.totalNotes.text = "${notes.size.toString()} notes"
                    adapter.setData(notes)

                }
            }else{
                binding.filterNotes.setImageResource(R.drawable.down_arrow)
                viewModel.getNotes().observe(viewLifecycleOwner) { notes ->

                    binding.totalNotes.text = "${notes.size.toString()} notes"
                    adapter.setData(notes)

                }
            }
            isDown = !isDown
        }


        binding.rvViewAllNotes.adapter = adapter

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = requireActivity().getSharedPreferences("myPref", Context.MODE_PRIVATE)
        sharedPrefLiveData = SharedPrefLiveData(sharedPref, "isGrid", true)


        isGrid = sharedPref.getBoolean("isGrid", true)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    private fun updateMenuVisibility() {
        val deleteMenuItem = menu.findItem(R.id.deleteIcon)
        deleteMenuItem.isVisible = selectedNotes.isNotEmpty()
        if(selectedNotes.isEmpty()){
            deleteMenuItem.isVisible = false
        }
    }

    private fun deleteSelectedNotes() {
        for (note in selectedNotes) {
            viewModel.deleteNote(note)
        }
        selectedNotes.clear()
        updateMenuVisibility()
        Toast.makeText(activity, "Selected notes deleted", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        this.menu = menu
        updateMenuVisibility()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.deleteIcon -> {
                deleteSelectedNotes()
                return true
            }
            R.id.search -> {
                Toast.makeText(activity, "Search", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.grid -> {
                isGrid = true
                binding.rvViewAllNotes.layoutManager = GridLayoutManager(context, 2)
                saveToSharedPref()
                return true
            }
            R.id.linear -> {
                isGrid = false
                binding.rvViewAllNotes.layoutManager = LinearLayoutManager(context)
                saveToSharedPref()
                return true
            }
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun saveToSharedPref() {
        editor = sharedPref.edit()
        editor.apply {
            putBoolean("isGrid", isGrid)
            commit()
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toggle = ActionBarDrawerToggle(
            requireActivity(),
            binding.drawerLayout,
            binding.toolbar,
            R.string.open,
            R.string.close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

    }

    override fun onItemClickListener(position: Int) {
        if (isSelectionMode) {
            val note = adapter.getData()[position]
            if (adapter.isSelected(position)) {
                // Item is already selected, unselect it
                adapter.deselectItem(position)
                selectedNotes.remove(note)
            } else {
                // Item is not selected, select it
                adapter.selectItem(position)
                selectedNotes.add(note)
            }
            updateMenuVisibility()
        } else {
            val note = adapter.getData()[position]
            val action = ViewAllNotesFragmentDirections.actionViewAllNotesFragmentToEditNoteFragment(note)
            findNavController().navigate(action)
        }
    }

    override fun onItemLongClickListener(position: Int): Boolean {
        if (!isSelectionMode) {
            isSelectionMode = true
            val note = adapter.getData()[position]
            adapter.selectItem(position)
            selectedNotes.add(note)
            updateMenuVisibility()
        }
        return true
    }
    override fun onSelectionChanged(isSelected: Boolean) {
        isSelectionMode = isSelected
        updateMenuVisibility()

        // Clear the selected notes if all are unselected
        if (!isSelected) {
            selectedNotes.clear()
        }
    }

}
