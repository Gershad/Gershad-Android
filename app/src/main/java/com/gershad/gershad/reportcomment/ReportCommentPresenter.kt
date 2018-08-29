package com.gershad.gershad.reportcomment

import android.content.Context
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.AmazonServiceProvider
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.model.Report
import com.gershad.gershad.model.ReportGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/*
 * Implementation of presenter that loads saved locations.
 *
 */
class ReportCommentPresenter(context: Context, private val reportsFragment: ReportCommentContract.View) : ReportCommentContract.Presenter, Injects<Module> {
    private val repository by required { repository }
    private var amazonServiceProvider: AmazonServiceProvider by required { amazonServiceProvider }

    init {
        reportsFragment.presenter = this
        inject(BaseApplication.module(context))
    }

    override fun updateReport(report: Report) {
        repository.updateReport(report)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    result ->
                    if (reportsFragment.isActive) {
                        reportsFragment.onReportUpdateSuccess(result)
                    }
                }, { _ ->
                    if (reportsFragment.isActive) {
                        reportsFragment.onReportUpdateFail()
                    }
                })
    }

    /*
 * @return Report if user contributed to report group, or null otherwise.
 */
    override fun findMyReportIn(reportGroup: ReportGroup) : Report? {
        return reportGroup.members.find { it.reporterToken == getToken() }
    }

    override fun getToken() : String {
        return amazonServiceProvider.cognitoIdentity
    }
}