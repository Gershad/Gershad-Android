package com.gershad.gershad.reports

import android.content.Context
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.model.ReportBounds
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/*
 * Implementation of presenter that loads saved locations.
 *
 */
class ReportsPresenter(context: Context, private val reportsFragment: ReportsContract.View) : ReportsContract.Presenter, Injects<Module> {
    private val repository by required { repository }

    init {
        reportsFragment.presenter = this
        inject(BaseApplication.module(context))
    }

    override fun loadItems(bounds: ReportBounds) {
        repository.getReports(bounds)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    result ->
                    if (reportsFragment.isActive) {
                        reportsFragment.onItemsLoaded(result)
                    }
                }, { _ ->
                    if (reportsFragment.isActive) {
                        reportsFragment.onItemsLoadFailed()
                    }
                })
    }
}