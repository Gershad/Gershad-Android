package com.gershad.gershad.savedlocationsdeletedialog

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.gershad.gershad.R
import com.gershad.gershad.event.RxBus
import kotlinx.android.synthetic.main.fragment_saved_locations_delete.*
import org.parceler.Parcels

/**
 * Saved locations view.
 */
class SavedLocationsDeleteDialogFragment : DialogFragment(), SavedLocationsDeleteDialogContract.View {

    override lateinit var presenter: SavedLocationsDeleteDialogContract.Presenter

    override var isActive: Boolean = false
        get() = isAdded

    companion object {
        fun newInstance() = SavedLocationsDeleteDialogFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_saved_locations_delete, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog.window.setLayout(MATCH_PARENT, MATCH_PARENT)

        confirm_delete.setOnClickListener {
            presenter.deleteItem(Parcels.unwrap(arguments!!.getParcelable("LOCATION")))
        }

        cancel_delete.setOnClickListener {
            dismiss()
        }

    }

    override fun onStart() {
        super.onStart()
        dialog?.let { it.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
    }


    override fun onItemDeleted() {
        RxBus.publish(Parcels.unwrap(arguments!!.getParcelable("LOCATION")))
        dismiss()
    }
}