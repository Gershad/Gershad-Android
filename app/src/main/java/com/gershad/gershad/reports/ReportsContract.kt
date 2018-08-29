package com.gershad.gershad.reports

import android.content.Context
import android.support.v4.app.FragmentActivity
import com.gershad.gershad.BasePresenter
import com.gershad.gershad.BaseView
import com.gershad.gershad.model.ReportBounds
import com.gershad.gershad.model.ReportGroup


interface ReportsContract {
    interface View : BaseView<Presenter> {

        val isActive: Boolean

        fun onItemsLoaded(items: List<ReportGroup>)

        fun onItemsLoadFailed()
    }

    interface Adapter {

        fun getContext(): Context?

        fun getActivity(): FragmentActivity?
    }

    interface Presenter : BasePresenter {

        fun loadItems(bounds: ReportBounds)
    }
}