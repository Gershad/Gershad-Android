package com.gershad.gershad.savedlocations

import android.content.Context
import android.support.v4.app.FragmentActivity
import com.gershad.gershad.BasePresenter
import com.gershad.gershad.BaseView
import com.gershad.gershad.model.SavedLocation


interface SavedLocationsContract {
    interface View : BaseView<Presenter> {

        val isActive: Boolean

        fun onItemsLoaded(items: List<SavedLocation>)

        fun onItemAdded(item: SavedLocation)
    }

    interface Adapter {

        fun getContext(): Context?

        fun getActivity(): FragmentActivity?
    }

    interface Presenter : BasePresenter {
        fun loadItems()
    }
}