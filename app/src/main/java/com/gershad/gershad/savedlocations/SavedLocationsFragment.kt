package com.gershad.gershad.savedlocations

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.gershad.gershad.R
import com.gershad.gershad.event.RxBus
import com.gershad.gershad.mapselector.MapSelectorActivity
import com.gershad.gershad.model.SavedLocation
import kotlinx.android.synthetic.main.fragment_saved_locations.*

/**
 * Saved (favourited) locations view.
 */
class SavedLocationsFragment : Fragment(), SavedLocationsContract.View, SavedLocationsContract.Adapter {

    override lateinit var presenter: SavedLocationsContract.Presenter
    lateinit var adapter: RecyclerView.Adapter<SavedLocationsAdapter.SavedLocationsViewHolder>
    private var listOfItems: ArrayList<SavedLocation> = ArrayList()
    private lateinit var emptyView: LinearLayout

    override var isActive: Boolean = false
        get() = isAdded

    companion object {
        fun newInstance() = SavedLocationsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_saved_locations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fab.setOnClickListener { startActivity(Intent(context, MapSelectorActivity::class.java)) }
        emptyView = empty_view
        swipe_refresh.isEnabled = true
        swipe_refresh.isRefreshing = true
    }

    override fun onResume() {
        super.onResume()
        presenter.loadItems()
    }


    override fun onItemsLoaded(items: List<SavedLocation>) {
        swipe_refresh.isRefreshing = false
        swipe_refresh.isEnabled = false
        if (items.isNotEmpty()) {
            listOfItems = ArrayList(items.reversed())
            recycler_view.layoutManager = LinearLayoutManager(context)
            adapter = SavedLocationsAdapter(this, listOfItems)
            recycler_view.adapter = adapter
            adapter.notifyDataSetChanged()
            emptyView.visibility = View.GONE
        } else {
            emptyView.visibility = View.VISIBLE
        }


        RxBus.listen(SavedLocation::class.java).subscribe({
            val index = listOfItems.indexOf(it)
            if (index > -1) {
                listOfItems.remove(it)
                adapter.notifyItemRemoved(index)
                if (listOfItems.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                }
            }
        })
    }
    
    /*
     * View contracts
     */
    override fun onItemAdded(item: SavedLocation) {
        listOfItems.add(item)
        adapter.notifyDataSetChanged()
    }
}
