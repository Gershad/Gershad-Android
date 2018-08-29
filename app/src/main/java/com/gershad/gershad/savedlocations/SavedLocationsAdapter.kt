package com.gershad.gershad.savedlocations

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.gershad.gershad.R
import com.gershad.gershad.extensions.savedLocationIcon
import com.gershad.gershad.map.MapActivity
import com.gershad.gershad.map.MapFragment
import com.gershad.gershad.model.SavedLocation
import com.gershad.gershad.savedlocationsdeletedialog.SavedLocationsDeleteDialogFragment
import com.gershad.gershad.savedlocationsdeletedialog.SavedLocationsDeleteDialogPresenter
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.popup_saved_locations.view.*
import org.parceler.Parcels
import java.text.NumberFormat
import java.util.*

class SavedLocationsAdapter(private val savedLocationsFragment: SavedLocationsContract.Adapter, private val savedLocations: List<SavedLocation>) : RecyclerView.Adapter<SavedLocationsAdapter.SavedLocationsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedLocationsAdapter.SavedLocationsViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_locatable, parent, false)
        return SavedLocationsViewHolder(itemView, savedLocationsFragment)
    }

    override fun onBindViewHolder(holder: SavedLocationsAdapter.SavedLocationsViewHolder, position: Int) {
        holder.bindView(position)
    }

    override fun onViewRecycled(holder: SavedLocationsViewHolder) {
        holder.clearView()
    }

    override fun getItemCount(): Int {
        return savedLocations.size
    }


    inner class SavedLocationsViewHolder(itemView: View, private val savedLocationsFragment: SavedLocationsContract.Adapter) : RecyclerView.ViewHolder(itemView), OnMapReadyCallback {

        private val cardView: CardView = itemView.findViewById(R.id.card_view)
        private val mapView: MapView = itemView.findViewById(R.id.map)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val options: AppCompatImageView = itemView.findViewById(R.id.options)
        private lateinit var map: GoogleMap
        private lateinit var latLng: LatLng

        init {
            with(mapView) {
                mapView.isClickable = false
                // Initialise the MapView
                onCreate(null)
                // Set the map ready callback to receive the GoogleMap object
                getMapAsync(this@SavedLocationsViewHolder)
            }
        }

        private fun setMapLocation() {
            if (!::map.isInitialized) return
            with(map) {
                uiSettings.isMapToolbarEnabled = false
                moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
                addMarker(MarkerOptions()
                        .position(latLng)
                        .icon(ContextCompat.getDrawable(savedLocationsFragment.getContext()!!, R.drawable.saved_location)!!.savedLocationIcon()))
                mapType = GoogleMap.MAP_TYPE_NORMAL
            }
        }

        override fun onMapReady(googleMap: GoogleMap?) {
            if (savedLocationsFragment.getContext() != null) {
                MapsInitializer.initialize(savedLocationsFragment.getContext())
                map = googleMap ?: return
                setMapLocation()
            }
        }

        fun bindView(position: Int) {
            savedLocations[position].let {
                val location = it
                latLng = LatLng(location.latitude, location.longitude)
                mapView.tag = this
                title.text = location.address
                cardView.setOnClickListener {
                    startMapActivity(location.latitude, location.longitude)
                }

                options.setOnClickListener {
                    setUpPopupWindow(it, savedLocationsFragment, location)
                }
                // We need to call setMapLocation from here because RecyclerView might use the
                // previously loaded maps
                setMapLocation()
            }
        }

        fun clearView() {
            with(map) {
                // Clear the map and free up resources by changing the map type to none
                clear()
                mapType = GoogleMap.MAP_TYPE_NONE
            }
        }

        private fun setUpPopupWindow(view: View, savedLocationsFragment: SavedLocationsContract.Adapter, savedLocation: SavedLocation) {
            val layoutInflater: LayoutInflater = view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popUpView = layoutInflater.inflate(R.layout.popup_saved_locations, null)



            val popUpWindow = PopupWindow(popUpView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            popUpView.share.setOnClickListener {
                val share = Intent(Intent.ACTION_SEND)
                share.type = "text/plain"

                val format = NumberFormat.getInstance(Locale.US)
                format.minimumFractionDigits = MapFragment.MAX_FRACTION_DIGITS
                format.maximumFractionDigits = MapFragment.MAX_FRACTION_DIGITS
                val url: String = MapFragment.MOBILE_URL_PREFIX + format.format(savedLocation.latitude) + "," + format.format(savedLocation.longitude)

                share.putExtra(Intent.EXTRA_TEXT, url)

                popUpView.context.startActivity(
                        Intent.createChooser(share, popUpView.context.getString(R.string.app_name))
                )
                popUpWindow.dismiss()
            }

            popUpView.delete.setOnClickListener {
                val savedLocationsDeleteDialogFragment = SavedLocationsDeleteDialogFragment.newInstance()
                SavedLocationsDeleteDialogPresenter(
                        savedLocationsFragment.getContext()!!,
                        savedLocationsDeleteDialogFragment
                )
                val bundle = Bundle()
                bundle.putParcelable("LOCATION", Parcels.wrap(savedLocation))
                savedLocationsDeleteDialogFragment.arguments = bundle
                val fm = (savedLocationsFragment.getActivity() as AppCompatActivity).supportFragmentManager
                savedLocationsDeleteDialogFragment.show(fm, savedLocationsDeleteDialogFragment::javaClass.name)
                popUpWindow.dismiss()
            }

            popUpWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            popUpWindow.isOutsideTouchable = true
            popUpWindow.showAsDropDown(view, -110, -120)
        }

        private fun startMapActivity(latitude: Double, longitude: Double) {
            val intent = Intent(savedLocationsFragment.getContext(), MapActivity::class.java)
            intent.putExtra(MapFragment.LATITUDE, latitude)
            intent.putExtra(MapFragment.LONGITUDE, longitude)
            savedLocationsFragment.getContext()?.startActivity(intent)
        }

    }
}
