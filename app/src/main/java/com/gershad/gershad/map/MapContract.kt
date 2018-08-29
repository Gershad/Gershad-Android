package com.gershad.gershad.map

import android.content.Context
import android.support.v4.app.FragmentActivity
import com.gershad.gershad.BasePresenter
import com.gershad.gershad.BaseView
import com.gershad.gershad.google.GoogleApiClientContract
import com.gershad.gershad.model.Report
import com.gershad.gershad.model.ReportBounds
import com.gershad.gershad.model.ReportGroup
import com.gershad.gershad.model.SavedLocation


interface MapContract {
    interface View : GoogleApiClientContract.View, BaseView<Presenter> {

        val isActive: Boolean

        fun  onItemsLoaded(items: List<ReportGroup>)

        fun onItemAdded(item: Report)

        fun onItemRemoved(item: Report, group: ReportGroup)

        fun onItemUpdated(item: Report)

        fun onPoiLoaded(items: List<SavedLocation>)

        fun onPoiAdded(item: SavedLocation)

        fun onPoiDeleted(item: SavedLocation)
    }

    interface Adapter {

        fun getContext(): Context?

        fun getActivity(): FragmentActivity?
    }

    interface Presenter : GoogleApiClientContract.Presenter, BasePresenter {

        fun loadItems(bounds: ReportBounds)

        fun addItem(newItem: Report)

        fun updateItem(newItem: Report)

        fun deleteItem(item: Report, group: ReportGroup)

        fun loadPoi()

        fun poiAdded (item: SavedLocation)

        fun deletePoi(item: SavedLocation)

        fun findMyReportIn(reportGroup: ReportGroup): Report?

        fun getToken(): String

        fun getEndpointArn(): String
    }
}