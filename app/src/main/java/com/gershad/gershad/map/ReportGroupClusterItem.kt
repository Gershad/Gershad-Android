package com.gershad.gershad.map

import com.gershad.gershad.model.Report
import com.gershad.gershad.model.ReportGroup

/**
 * ClusterItem class to display [com.gershad.gershad.model.ReportGroup]. This means that "Report Groups" (all reports sharing a specific location)
 * are displayed with a single marker pin on the map.
 *
 * Using the [com.gershad.gershad.map.ReportClusterRenderer] allows these markers to be further grouped (clustered) at low zoom factor to prevent cluttering the map.
 *
 * Right now the Cluster Item behaviour (getAddress, getSnippet, getPosition) is handled by the [ReportGroup].
 */
typealias ReportGroupClusterItem = ReportGroup

fun ReportGroupClusterItem.add(report: Report): ReportGroupClusterItem {
    val cluster = this
    cluster.members.add(report)
    return cluster
}

fun ReportGroupClusterItem.remove(report: Report): ReportGroupClusterItem? {
    val cluster = this
    cluster.members.remove(report)
    return if (cluster.members.size > 0) cluster else null
}

