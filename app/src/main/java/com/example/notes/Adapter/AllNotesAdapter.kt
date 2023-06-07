import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.example.notes.Database.Note
import com.example.notes.Fragments.ViewAllNotesFragmentDirections
import com.example.notes.databinding.NoteItemBinding

class AllNotesAdapter(
    private var list: List<Note>,
    private val onClickListener: onItemClick,
    private val onLongClickListener: onItemLongClick
) : RecyclerView.Adapter<AllNotesAdapter.MyViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()
    private var firstItemSelected = false

    inner class MyViewHolder(val binding: NoteItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener, View.OnLongClickListener {

        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                if (selectedItems.isNotEmpty()) {
                    toggleSelection(position)
                } else {
                    onClickListener.onItemClickListener(position)
                }
            }
        }

        override fun onLongClick(p0: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onLongClickListener.onItemLongClickListener(position)
                return true
            }
            return false
        }

        private var onSelectionChangedListener: OnSelectionChangedListener? = null

        fun setOnSelectionChangedListener(listener: OnSelectionChangedListener) {
            onSelectionChangedListener = listener
        }
    }

     fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        notifyItemChanged(position)

        if (selectedItems.isEmpty()) {
            onSelectionCleared()
        }
    }



    private fun onSelectionCleared() {
        selectedItems.clear()
        firstItemSelected = false
        // Notify the fragment to update the menu visibility
        onSelectionChangedListener?.onSelectionChanged(false)
    }

    fun clearSelection() {
        selectedItems.clear()
        firstItemSelected = false
        notifyDataSetChanged()
    }

    private var onSelectionChangedListener: OnSelectionChangedListener? = null

    fun setOnSelectionChangedListener(listener: OnSelectionChangedListener) {
        onSelectionChangedListener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            NoteItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val note = list[position]
        if (note.title.isEmpty()) {
            holder.binding.title.text = note.content
        } else {
            holder.binding.title.text = note.title
            holder.binding.content.text = note.content
        }
        holder.binding.date.text = note.date

        holder.binding.root.setOnClickListener {
            if (selectedItems.isEmpty()) {
                val action =
                    ViewAllNotesFragmentDirections.actionViewAllNotesFragmentToEditNoteFragment(
                        note
                    )
                Navigation.findNavController(it).navigate(action)
            }
        }

        if (selectedItems.contains(position)) {
            holder.binding.checkButton.visibility = View.VISIBLE
        } else {
            holder.binding.checkButton.visibility = View.INVISIBLE
        }
    }
    fun getData(): List<Note> {
        return list
    }

    fun selectItem(position: Int) {
        if (!selectedItems.contains(position)) {
            selectedItems.add(position)
            notifyItemChanged(position)
            if (selectedItems.size >= 1) {
                // First item selected, notify the fragment to update the menu visibility
                onSelectionChangedListener?.onSelectionChanged(true)
            }
        }
    }

    fun getSelectedItems(): MutableSet<Int> {
        return selectedItems
    }

    fun setData(newData: List<Note>) {
        list = newData
        notifyDataSetChanged()
    }
    fun deselectItem(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
            notifyItemChanged(position)
            if (selectedItems.isEmpty()) {
                onSelectionChangedListener?.onSelectionChanged(false)
            }
        }
    }
    fun isSelected(position: Int): Boolean {
        return selectedItems.contains(position)
    }

    interface onItemClick {
        fun onItemClickListener(position: Int)
    }

    interface onItemLongClick {
        fun onItemLongClickListener(position: Int): Boolean
    }
    interface OnSelectionChangedListener {
        fun onSelectionChanged(isSelected: Boolean)
    }
}
