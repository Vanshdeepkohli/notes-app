import android.util.Log
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

    val selectedItems = mutableSetOf<Note>()

    inner class MyViewHolder(val binding: NoteItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener, View.OnLongClickListener {

        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            Log.e("@@@@", "inside OnClick block")
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                if (selectedItems.isEmpty()) {
                    val action =
                        ViewAllNotesFragmentDirections.actionViewAllNotesFragmentToEditNoteFragment(
                            list[position]
                        )
                    Navigation.findNavController(binding.root).navigate(action)
                } else {
                    Log.e("@@@@", "inside onClick Else block")
                    onClickListener.onItemClickListener(list[position])
                    selectedItems.add(list[position])
                }
            }
        }

        override fun onLongClick(p0: View?): Boolean {
            Log.e("@@@@", "inside onLongClick block")
            if (selectedItems.isEmpty()) {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    selectedItems.add(list[position])
                    onLongClickListener.onItemLongClickListener(list[position])
                    return true
                }
            }
            return false
        }

    }


    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
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

        if (note.isSelected == 0) {
            holder.binding.checkButton.visibility = View.GONE
        } else {
            holder.binding.checkButton.visibility = View.VISIBLE
        }

        if (note.title.isEmpty()) {
            holder.binding.title.text = note.content
        } else {
            holder.binding.title.text = note.title
            holder.binding.content.text = note.content
        }

        holder.binding.date.text = note.date


    }

    fun selectItem(note: Note) {
        selectedItems.add(note)
        notifyDataSetChanged()
    }


    fun setData(newData: List<Note>) {
        list = newData
        notifyDataSetChanged()
    }

    fun deselectItem(note: Note) {
        selectedItems.remove(note)
        notifyDataSetChanged()
    }

    interface onItemClick {
        fun onItemClickListener(note: Note)
    }

    interface onItemLongClick {
        fun onItemLongClickListener(note: Note): Boolean
    }

}
