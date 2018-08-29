package com.gershad.gershad.reports

import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gershad.gershad.R
import com.gershad.gershad.extensions.formatTime
import com.gershad.gershad.extensions.toDistance
import com.gershad.gershad.map.MapActivity
import com.gershad.gershad.map.MapFragment
import com.gershad.gershad.model.Report.ReportType
import com.gershad.gershad.model.ReportGroup
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.android.synthetic.main.item_report_snippet.view.*

class ReportsAdapter(private val reportsFragment: ReportsContract.Adapter, private val reports: List<ReportGroup>, private val origin: LatLng) : RecyclerView.Adapter<ReportsAdapter.ReportsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportsAdapter.ReportsViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_report_snippet, parent, false)
        return ReportsViewHolder(itemView, reportsFragment)
    }

    override fun onBindViewHolder(holder: ReportsAdapter.ReportsViewHolder, position: Int) {
        holder.bindItems(reports[position], origin)
    }

    override fun getItemCount(): Int {
        return reports.size
    }


    class ReportsViewHolder(itemView: View, private val reportsFragment: ReportsContract.Adapter) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(report: ReportGroup, origin: LatLng) {

            with(report) {
                itemView.timestamp.text = formatTime(lastUpdate)
                itemView.report_type_icon.setImageDrawable(ContextCompat.getDrawable(itemView.context, getDrawableType(this)))
                itemView.reports.text = String.format(reportsFragment.getContext()!!.getString(R.string.report_count), this.members.size)

                itemView.address.text = members[0].address
                itemView.distance.text = SphericalUtil.computeDistanceBetween(origin, position).toDistance(reportsFragment.getContext()!!)

                itemView.card_view.setOnClickListener {
                    startMapActivity(latitude, longitude)
                }
            }
        }

        private fun getDrawableType(report: ReportGroup): Int {

            return when (report.displayType) {
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

        private fun startMapActivity(latitude: Double, longitude: Double) {
            val intent = Intent(reportsFragment.getContext(), MapActivity::class.java)
            intent.putExtra(MapFragment.LATITUDE, latitude)
            intent.putExtra(MapFragment.LONGITUDE, longitude)
            reportsFragment.getContext()?.startActivity(intent)
        }
    }
}
