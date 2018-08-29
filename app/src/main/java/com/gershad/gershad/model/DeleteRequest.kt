package com.gershad.gershad.model


data class DeleteRequest(val id: Int, val token: String) {
    constructor(report: Report): this(report.id!!, report.reporterToken)

    constructor(savedLocation: SavedLocation): this(savedLocation.id!!, savedLocation.reporterToken)
}