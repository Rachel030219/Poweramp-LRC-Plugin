package net.rachel030219.poweramplrc

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_folders.*

class FoldersActivity: AppCompatActivity() {
    // determine whether going to add a folder
    var additionOngoing = false
    var recyclerAdapter: Adapter? = null

    var databaseHelper: FoldersDatabaseHelper? = null
    var folders: MutableList<FoldersDatabaseHelper.Companion.Folder> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folders)

        databaseHelper = FoldersDatabaseHelper(this)

        recyclerAdapter = Adapter()
        folder_add.setOnClickListener {
            additionOngoing = true
            startActivity(Intent(this, PathActivity::class.java).putExtra("request", PathActivity.REQUEST_FOLDER))
        }
        folder_recycler.apply {
            layoutManager = LinearLayoutManager(this@FoldersActivity)
            adapter = recyclerAdapter
        }
        permission_folder_include.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.getDefaultSharedPreferences(this@FoldersActivity).edit().putBoolean("subDir", isChecked).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        folders = databaseHelper!!.fetchFolders()
        permission_folder_include.isChecked = PreferenceManager.getDefaultSharedPreferences(this@FoldersActivity).getBoolean("subDir", false)
        if (additionOngoing) {
            additionOngoing = false
            recyclerAdapter?.notifyDataSetChanged()
        }
    }

    inner class Adapter: RecyclerView.Adapter<Holder> () {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(LayoutInflater.from(this@FoldersActivity).inflate(R.layout.item_folder, parent, false))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.name.text = folders[position].name
            holder.path.text = folders[position].path
            holder.deleteButton.setOnClickListener {
                databaseHelper!!.removeFolder(folders[position])
                folders.removeAt(position)
                notifyItemRemoved(position)
            }
        }

        override fun getItemCount(): Int {
            return folders.size
        }
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.folder_name)
        val path: TextView = itemView.findViewById(R.id.folder_path)
        val deleteButton: ImageButton = itemView.findViewById(R.id.folder_delete)
    }
}