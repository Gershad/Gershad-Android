package com.gershad.gershad.savedlocationsdeletedialog

import android.content.Context
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.model.DeleteRequest
import com.gershad.gershad.model.SavedLocation
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/*
 * Implementation of presenter that loads saved locations.
 *
 */
class SavedLocationsDeleteDialogPresenter(context: Context, private val savedLocationsDeleteDialog : SavedLocationsDeleteDialogContract.View) : SavedLocationsDeleteDialogContract.Presenter, Injects<Module> {

    private val repository by required { repository }

    init {
        savedLocationsDeleteDialog.presenter = this
        inject(BaseApplication.module(context))
    }

    override fun deleteItem(targetItem: SavedLocation) {
            repository.deletePoi(DeleteRequest(targetItem))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (savedLocationsDeleteDialog.isActive) {
                            savedLocationsDeleteDialog.onItemDeleted()
                        }
                    }) {
                        if (savedLocationsDeleteDialog.isActive) {
                        }
                    }
    }
}