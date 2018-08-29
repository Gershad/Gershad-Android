package com.gershad.gershad.mapselector

import com.gershad.gershad.BasePresenter
import com.gershad.gershad.BaseView
import com.gershad.gershad.google.GoogleApiClientContract
import com.gershad.gershad.model.SavedLocation


interface MapSelectorContract {
    interface View : GoogleApiClientContract.View, BaseView<Presenter> {
        fun onItemAdded(response: SavedLocation?)

        fun onItemsLoaded(result: List<SavedLocation>?)

        val isActive: Boolean
    }


    interface Presenter : GoogleApiClientContract.Presenter, BasePresenter {
        fun addItem(newItem: SavedLocation)

        fun loadItems()

        fun getEndpointArn(): String

        fun getToken(): String
    }
}