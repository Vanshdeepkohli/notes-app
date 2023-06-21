package com.example.notes.Fragments

import AllNotesAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.util.query
import com.example.notes.Database.Note
import com.example.notes.Database.NoteDao
import com.example.notes.Database.NoteDatabase
import com.example.notes.R
import com.example.notes.Repository.NoteRepository
import com.example.notes.SharedPreference.SharedPrefLiveData
import com.example.notes.ViewModel.NoteViewModel
import com.example.notes.databinding.FragmentViewAllNotesBinding
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.internal.GoogleServices
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.storage.StorageReference

class ViewAllNotesFragment : Fragment(), MenuProvider, AllNotesAdapter.onItemClick, AllNotesAdapter.onItemLongClick {

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
    private lateinit var selectedNotes :MutableList<Int>
    var count = 0


    // auth
    private lateinit var auth : FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    // NOTIFICATION
    private val CHANNEL_ID = "Sync 1"
    private val CHANNEL_NAME = "Sync"
    private val NOTIFICATION_ID = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentViewAllNotesBinding.inflate(inflater, container, false)
        adapter = AllNotesAdapter(emptyList(), this, this)

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)

        toggle = ActionBarDrawerToggle(activity, binding.drawerLayout, R.string.open, R.string.close)
        toggle.syncState()

        userDao = NoteDatabase.getDatabase(requireContext()).getDao()
        repository = NoteRepository(userDao)
        viewModel = NoteViewModel(repository)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewModel.getNotes().observe(viewLifecycleOwner) { notes ->
            binding.totalNotes.text = "${notes.size} notes"
            adapter.setData(notes)
            oldNotes = notes as ArrayList<Note>
            Log.e("@@@@","Inside getNotes() from main")
        }

        sharedPrefLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.rvViewAllNotes.layoutManager = GridLayoutManager(context, 3)
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
                    // Create new GoogleSignInOptions with requestIdToken and requestEmail options
                    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()

                    // Create new googleSignInClient with updated options
                    val googleSignInClient = GoogleSignIn.getClient(requireActivity(), googleSignInOptions)

                    // Start sign-in activity with the new client
                    googleSignInClient.signOut().addOnCompleteListener {
                        launcher.launch(googleSignInClient.signInIntent)
                    }
                }

                R.id.sync -> {
                    auth.currentUser?.let {
                        sendNotification("Syncing...","notes are being uploading...")
                    }
                    if(auth.currentUser == null) {
                        Toast.makeText(requireContext(), "Please login with your gmail first",Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.logOut -> {
                    binding.progressBar.isIndeterminate = true
                    auth.signOut()
                    if(auth.currentUser == null){
                        binding.navView.getHeaderView(0).findViewById<TextView>(R.id.name).text = ""
                        binding.navView.getHeaderView(0).findViewById<TextView>(R.id.email).text = ""
                        Toast.makeText(activity, "Logged Out", Toast.LENGTH_SHORT).show()
                        binding.progressBar.isIndeterminate = false
                    }else{
                        Toast.makeText(activity, "Logout failed", Toast.LENGTH_SHORT).show()
                    }
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

        var newListWhenSearching = mutableListOf<Note>()
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                newListWhenSearching.clear()
                if(!query.isNullOrEmpty()){
                    for(note in oldNotes){
                        if(note.title.contains(query, ignoreCase = true) || note.content.contains(query, ignoreCase = true)){
                            newListWhenSearching.add(note)
                            adapter.setData(newListWhenSearching)
                        }
                    }
                }
                adapter.setData(oldNotes)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newListWhenSearching.clear()
                if(!newText.isNullOrEmpty()){
                    for(note in oldNotes){
                        if(note.title.contains(newText, ignoreCase = true) || note.content.contains(newText, ignoreCase = true)){
                            newListWhenSearching.add(note)
                            adapter.setData(newListWhenSearching)
                        }
                    }
                }else{
                    adapter.setData(oldNotes)
                }
                return true
            }

        })

        return binding.root
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(title:String, text:String) {
        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
        .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()

        val manager = NotificationManagerCompat.from(requireContext())
        manager.notify(NOTIFICATION_ID,notification)

        sendNotesToStorage()
    }

    private fun sendNotesToStorage() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val currentUserId = auth.currentUser?.uid
            val userNotesRef = storageRef.child("notes").child(currentUserId!!)

            for(note in oldNotes){
                val noteTitle = note.title
                val noteContent = note.content
                val noteDate = note.date
                val noteId = note.id
                val noteIsSelected = note.isSelected


                // Convert note content to byte array
                val noteContentByteArray = noteContent.toByteArray()
                // unique file name for the note
                val noteFileName = "${note.id}-txt"

                // Create a reference to the file in Firebase Storage
//                val noteRef = database.reference.child("users/${auth.currentUser?.email}/notes/$noteFileName")
//                currentUserNotesRef = database.reference.child("users").child(auth.currentUser!!.uid).child("notes")
                val noteFileRef = userNotesRef.child(noteFileName)

                // Upload the note content to Firebase Storage
                if(noteContentByteArray.isNotEmpty()) {
                    noteFileRef.putBytes(noteContentByteArray)
                        .addOnSuccessListener {
                            // Handle successful upload
                            Log.d("@@@@", "Note uploaded successfully")
                        }
                        .addOnFailureListener { e ->
                            // Handle failed upload
                            Log.e("@@@@", "Failed to upload note: $e")
                        }
                }

            }

        }catch (e:Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(context,"Failed uploading ${e.message}",Toast.LENGTH_SHORT).show()
                Log.e("@@@@", e.toString())
            }
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
        binding.progressBar.isIndeterminate = true
        if (result.resultCode == Activity.RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }else{
            Toast.makeText(context,"Sign in failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful){
            val account = task.getResult(ApiException::class.java)
            if (account != null){
                updateUI(account)
            }
        }else{
            Toast.makeText(context, task.exception.toString() , Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken , null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful){
                binding.progressBar.isIndeterminate = false
                binding.navView.getHeaderView(0).findViewById<TextView>(R.id.name).text = account.displayName
                binding.navView.getHeaderView(0).findViewById<TextView>(R.id.email).text = account.email
            }else{
                Toast.makeText(context, it.exception.toString() , Toast.LENGTH_SHORT).show()

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = requireActivity().getSharedPreferences("myPref", Context.MODE_PRIVATE)
        sharedPrefLiveData = SharedPrefLiveData(sharedPref, "isGrid", true)


        isGrid = sharedPref.getBoolean("isGrid", true)

        selectedNotes = mutableListOf<Int>()

        createNotificationChannel()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        selectedNotes.clear()
        adapter.clearSelection()
        for(n in oldNotes){
            viewModel.updateIsSelected(n.id!!,0)
        }
    }

    override fun onStart() {
        super.onStart()
        for(n in oldNotes){
            viewModel.updateIsSelected(n.id!!,0)
        }

        if(auth.currentUser != null){
            binding.navView.getHeaderView(0).findViewById<TextView>(R.id.name).text = auth.currentUser!!.displayName
            binding.navView.getHeaderView(0).findViewById<TextView>(R.id.email).text = auth.currentUser!!.email
        }

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
                toggleSearchBar()
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

    private fun toggleSearchBar() {
        if(binding.searchView.visibility == View.INVISIBLE){
            binding.searchView.visibility = View.VISIBLE
            binding.dateModifier.visibility = View.GONE
            binding.filterNotes.visibility = View.GONE
        }else{
            binding.toolbar.title = "All notes"
            binding.searchView.visibility = View.INVISIBLE
            binding.dateModifier.visibility = View.VISIBLE
            binding.filterNotes.visibility = View.VISIBLE
            binding.searchView.setQuery("", false)
        }
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

    override fun onItemClickListener(note : Note) {
        val note = note
        if(selectedNotes.contains(note.id)){
            Log.e("@@@@","Inside selectedNotes.contains(note)")
            viewModel.updateIsSelected(note.id!!,0)
            selectedNotes.remove(note.id)
            adapter.deselectItem(note)
            count--
            updateMenuVisibility()
        }else{
            Log.e("@@@@","Inside selectedNotes.contains(note) else")
            viewModel.updateIsSelected(note.id!!,1)
            selectedNotes.add(note.id)
            adapter.selectItem(note)
            count++
            updateMenuVisibility()
        }
        if(count == 0) {
            selectedNotes.clear()
            adapter.clearSelection()
            updateMenuVisibility()
            adapter.selectedItems.clear()
        }
        Log.e("@@@@","$selectedNotes")
    }

    override fun onItemLongClickListener(note : Note): Boolean {
            val note = note
        if(!selectedNotes.contains(note.id)){
            viewModel.updateIsSelected(note.id!!,1)
            selectedNotes.add(note.id)
            count++
            updateMenuVisibility()
        }
        Log.e("@@@@","inside onLongClick $selectedNotes")
        if(count == 0) {
            selectedNotes.clear()
            adapter.clearSelection()
            updateMenuVisibility()
            adapter.selectedItems.clear()
        }
        return true
    }


    private fun deleteSelectedNotes() {
        val currentUserId = auth.currentUser?.uid
        for (note in selectedNotes) {
            viewModel.deleteAt(note)
            val deleteRef =  storageRef.child("notes").child(currentUserId!!).child("$note-txt")
            deleteRef.delete().addOnSuccessListener {
                Toast.makeText(requireContext(),"deleted from cloud also",Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(),"couldn't from cloud",Toast.LENGTH_SHORT).show()
            }
        }
        count = 0
        selectedNotes.clear()
        adapter.clearSelection()
        updateMenuVisibility()
        adapter.selectedItems.clear()
        Toast.makeText(activity, "Selected notes deleted", Toast.LENGTH_SHORT).show()
    }

    private fun updateMenuVisibility() {
        if(count == 0) {
            selectedNotes.clear()
            adapter.clearSelection()
            adapter.selectedItems.clear()
        }
        if(selectedNotes.isEmpty()){
            menu.findItem(R.id.deleteIcon).isVisible = false
            menu.findItem(R.id.search).isVisible = true
        }else{
            menu.findItem(R.id.deleteIcon).isVisible = true
            menu.findItem(R.id.search).isVisible = false
        }
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "shows you about uploading notes to cloud"

            val notificationManager  = requireActivity().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}
