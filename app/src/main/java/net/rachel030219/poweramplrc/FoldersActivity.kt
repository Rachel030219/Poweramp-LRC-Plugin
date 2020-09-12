package net.rachel030219.poweramplrc

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class FoldersActivity: AppCompatActivity() {
    // TODO: implement SQLiteOpenHelper to save names and paths & add a entrance(preference) to this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folders)
    }

    class Adapter: RecyclerView.Adapter<Holder> () {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            TODO("Not yet implemented")
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            TODO("Not yet implemented")
        }

        override fun getItemCount(): Int {
            TODO("Not yet implemented")
        }
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.folder_name)
        val path: TextView = itemView.findViewById(R.id.folder_path)
        val deleteButton: ImageButton = itemView.findViewById(R.id.folder_delete)
    }
}