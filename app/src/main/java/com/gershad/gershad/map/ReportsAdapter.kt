package com.gershad.gershad.map

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gershad.gershad.R
import com.gershad.gershad.extensions.formatTime
import com.gershad.gershad.model.Report
import com.gershad.gershad.model.Report.ReportType
import com.gershad.gershad.model.ReportGroup
import kotlinx.android.synthetic.main.item_report_type.view.*

class ReportsAdapter(private val reportsFragment: MapContract.Adapter, private val reports: ArrayList<Report>) : RecyclerView.Adapter<ReportsAdapter.ReportsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportsAdapter.ReportsViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_report_type, parent, false)
        return ReportsViewHolder(itemView, reportsFragment)
    }

    override fun onBindViewHolder(holder: ReportsAdapter.ReportsViewHolder, position: Int) {
        holder.bindItems(reports[position])
    }

    override fun getItemCount(): Int {
        return reports.size
    }


    class ReportsViewHolder(itemView: View, private val reportsFragment: MapContract.Adapter) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(report: Report) {

            with(report) {
                itemView.timestamp.text = formatTime(timestamp)
                itemView.report_type_icon.setImageDrawable(ContextCompat.getDrawable(itemView.context, getDrawableType(report)))
                itemView.type.text = reportsFragment.getContext()!!.getString(getReportType(report))

                itemView.description_layout.visibility = View.GONE
                if (!report.comments.isNullOrEmpty()) {
                    itemView.description_layout.visibility = View.VISIBLE
                    itemView.description.text = report.comments
                }
            }
        }

        private fun getDrawableType(report: Report): Int {

            return when (report.type) {
                ReportType.VAN -> {
                    R.drawable.report_van
                }
                ReportType.GASHT -> {
                    R.drawable.report_police
                }
                ReportType.STOP -> {
                    R.drawable.report_stop
                }
                else /* or UNSPECIFIED */ -> {
                    R.drawable.logo // Todo or some other general icon. This shouldn't happen though!
                }
            }
        }

        private fun getReportType(report: Report): Int {

            return when (report.type) {
                ReportType.VAN -> {
                    R.string.van
                }
                ReportType.GASHT -> {
                    R.string.police
                }
                ReportType.STOP -> {
                    R.string.stop
                }
                else -> {
                    R.string.unspecified
                }
            }
        }
    }

    fun updateMembers(reportGroup: ReportGroup) {
        reports.clear()
        reports.addAll(reportGroup.members)
        notifyDataSetChanged()
    }
}
