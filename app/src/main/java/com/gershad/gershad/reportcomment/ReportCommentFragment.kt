package com.gershad.gershad.reportcomment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.gershad.gershad.R
import com.gershad.gershad.map.MapActivity
import com.gershad.gershad.model.Report
import com.gershad.gershad.model.ReportGroup
import kotlinx.android.synthetic.main.fragment_report_comment.*
import org.parceler.Parcels


/**
 * Saved (favourited) locations view.
 */
class ReportCommentFragment : Fragment(), ReportCommentContract.View {
    override lateinit var presenter: ReportCommentContract.Presenter

    private var reportGroup: ReportGroup? = null
    private var report: Report? = null


    override var isActive: Boolean = false
        get() = isAdded

    companion object {
        const val REPORT = "REPORT"

        fun newInstance() = ReportCommentFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View? = inflater.inflate(R.layout.fragment_report_comment, container, false)
        reportGroup = Parcels.unwrap(arguments?.getParcelable(REPORT))
        reportGroup?.let {
            report = presenter.findMyReportIn(it)
        }


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        explanation_error.visibility = View.GONE
        report?.comments?.let {
            explanation.setText(it, TextView.BufferType.EDITABLE)
        }

        submit_feedback.setOnClickListener {
            report?.let {
                if (explanation.text.isEmpty()) {
                    explanation_error.visibility = View.VISIBLE
                } else {
                    submit_feedback.isEnabled = false
                    explanation.isEnabled = false
                    progress_bar.visibility = View.VISIBLE
                    explanation_error.visibility = View.GONE
                    it.comments = explanation.text.toString()
                    presenter.updateReport(it)
                }
            }
        }
    }

    override fun onReportUpdateSuccess(item: Report) {
        val intent = MapActivity.newIntent(context!!)
        intent.putExtra(REPORT, Parcels.wrap(reportGroup))
        startActivity(intent)
    }

    override fun onReportUpdateFail() {
        submit_feedback.isEnabled = true
        explanation.isEnabled = true
        Toast.makeText(context, R.string.time_out_retry_check_connection, Toast.LENGTH_SHORT).show()
    }
}
