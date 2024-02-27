package com.example.facemaker
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.MyViewHolder>() {

    private var data: MutableMap<String, Long> = mutableMapOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.leaderboard_entry, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val username = data.keys.elementAt(position)
        val score = data.values.elementAt(position)

        holder.user.text = username
        holder.score.text = score.toString()
    }

    override fun getItemCount() = data.size

    fun setData(newData: MutableMap<String, Long>) {
        data = newData
        notifyDataSetChanged()
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val user: TextView = itemView.findViewById(R.id.leaderboarduser)
        val score : TextView = itemView.findViewById(R.id.leaderboardvalue)
    }
}