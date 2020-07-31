package me.shiki.livepusher

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.shiki.livepusher.filter.FilterSurfaceView
import me.shiki.livepusher.filter.model.Filter

/**
 * FilterAdapter
 *
 * @author shiki
 * @date 2020/7/21
 *
 */
class FilterAdapter(val context: Context, private val list: Array<Filter>) :
    RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    var onItemClickListener: ((position: Int, filter: Filter) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.item_filter, parent, false)
        return FilterViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val data = list[position]
        holder.fsv.isAutoEglDestroyed = false
        holder.fsv.setTextureIdAndEglcontext(data.render, data.eglContext)
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(position, list[position])
        }
    }

    class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fsv: FilterSurfaceView = itemView.findViewById(R.id.fsv)
    }
}