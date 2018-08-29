package com.gershad.gershad.reportcomment

import com.gershad.gershad.BasePresenter
import com.gershad.gershad.BaseView
import com.gershad.gershad.model.Report
import com.gershad.gershad.model.ReportGroup


interface ReportCommentContract {
    interface View : BaseView<Presenter> {

        val isActive: Boolean

        fun onReportUpdateSuccess(item: Report)

        fun onReportUpdateFail()
    }

    interface Presenter : BasePresenter {

        fun updateReport(report: Report)

        fun findMyReportIn(reportGroup: ReportGroup): Report?

        fun getToken(): String
    }
}