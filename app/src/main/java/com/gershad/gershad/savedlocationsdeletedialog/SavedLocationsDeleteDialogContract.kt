package com.gershad.gershad.savedlocationsdeletedialog

import com.gershad.gershad.BasePresenter
import com.gershad.gershad.BaseView
import com.gershad.gershad.model.SavedLocation

interface SavedLocationsDeleteDialogContract {
    interface View : BaseView<Presenter> {

        val isActive: Boolean

        fun onItemDeleted()
    }

    interface Presenter : BasePresenter {

        fun deleteItem(targetItem: SavedLocation)
    }
}