package com.gershad.gershad.savedlocations

import android.content.Context
import com.gershad.gershad.AmazonServiceProvider
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.dependency.Module
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/*
 * Implementation of presenter that loads saved locations.
 *
 */
class SavedLocationsPresenter(context: Context, savedLocationsFragment : SavedLocationsContract.View) : SavedLocationsContract.Presenter, Injects<Module> {
    private val repository by required { repository }
    private var amazonServiceProvider: AmazonServiceProvider by required { amazonServiceProvider }
    private val savedLocationFragment = savedLocationsFragment

    init {
        savedLocationsFragment.presenter = this
        inject(BaseApplication.module(context))
    }

    override fun loadItems() {
        repository.getPoi(amazonServiceProvider.cognitoIdentity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    result ->
                    if (savedLocationFragment.isActive) {
                        savedLocationFragment.onItemsLoaded(result)
                    }
                }, { _ ->
                    if (savedLocationFragment.isActive) {
                        savedLocationFragment.onItemsLoaded(ArrayList())
                    }
                })
    }
}