package com.gershad.gershad.map

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.support.v4.content.ContextCompat
import android.util.SparseArray
import android.view.ViewGroup
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.R
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.model.Report.ReportType
import com.gershad.gershad.model.ReportGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import com.google.maps.android.ui.SquareTextView
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject

/**
 * Custom cluster renderer to show unique icons dependent on item type.
 */
class ReportClusterRenderer(val context: Context, map: GoogleMap, clusterManager: ClusterManager<ReportGroupClusterItem>)
    : DefaultClusterRenderer<ReportGroupClusterItem>(context, map, clusterManager), Injects<Module> {

    // Only initialize if needed. Use TypedArray to get and retain resource IDs, then recycle.
    private val gashtIcons by lazy { getDrawableArray(ReportType.GASHT) }
    private val vanIcons by lazy { getDrawableArray(ReportType.VAN) }
    private val stopIcons by lazy { getDrawableArray(ReportType.STOP) }
    private val unspecifiedIcons by lazy { getDrawableArray(ReportType.UNSPECIFIED) }


    private val buckets: IntArray = intArrayOf(10, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 150, 200, 300, 400, 500, 750, 1000)
    private var coloredCircleBackground: ShapeDrawable? = null
    private var density: Float = context.resources.displayMetrics.density
    private val iconGenetaor: IconGenerator = IconGenerator(context)
    private val icons: SparseArray<BitmapDescriptor>

    init {
        inject(BaseApplication.module(context))
        this.iconGenetaor.setContentView(this.makeSquareTextView(context))
        this.iconGenetaor.setTextAppearance(com.google.maps.android.R.style.amu_ClusterIcon_TextAppearance)
        this.iconGenetaor.setBackground(this.makeClusterBackground())
        this.icons = SparseArray()
    }

    override fun onBeforeClusterRendered(cluster: Cluster<ReportGroup>, markerOptions: MarkerOptions) {
        super.onBeforeClusterRendered(cluster, markerOptions)
        val bucket = this.getBucket(cluster)
        var descriptor: BitmapDescriptor? = this.icons.get(bucket)
        if (descriptor == null) {
            this.coloredCircleBackground!!.paint.color = this.getColor(bucket)
            descriptor = BitmapDescriptorFactory.fromBitmap(iconGenetaor.makeIcon(this.getClusterText(bucket)))
            this.icons.put(bucket, descriptor)
        }
        markerOptions.icon(descriptor)
    }

    override fun onBeforeClusterItemRendered(item: ReportGroupClusterItem?, markerOptions: MarkerOptions?) {
        //super.onBeforeClusterItemRendered(item, markerOptions)
        val iconId: Int = getClusterIcon(item!!.displayType, item.reportCount)

        // Known issue with BitmapDescriptorFactory.fromResource and Vector drawables causing a crash.
        val drawable: Drawable = ContextCompat.getDrawable(context, iconId)!!
        val bitmap: Bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        markerOptions?.icon(BitmapDescriptorFactory.fromBitmap(bitmap))?.alpha(item.fadeValue)?.zIndex(100.0f)?.title("")?.snippet("")
    }

    private fun makeClusterBackground(): LayerDrawable {
        this.coloredCircleBackground = ShapeDrawable(OvalShape())
        val outline = ShapeDrawable(OvalShape())
        outline.paint.color = -2130706433
        val background = LayerDrawable(arrayOf<Drawable>(outline, this.coloredCircleBackground!!))
        val strokeWidth = (this.density * 3.0f).toInt()
        background.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth)
        return background
    }

    override fun getClusterText(bucket: Int): String {
        return if (bucket < buckets[0]) convertNumbers(bucket.toString()) else "+" + convertNumbers(Integer.toString(bucket))
    }

    override fun getColor(clusterSize: Int): Int {
        val hueRange = 50.0f
        val size = Math.min(clusterSize.toFloat(), 50f)
        val hue = hueRange - size
        return Color.HSVToColor(floatArrayOf(hue, 0.83f, 1.0f))
    }

    private fun makeSquareTextView(context: Context): SquareTextView {
        val squareTextView = SquareTextView(context)
        val layoutParams = ViewGroup.LayoutParams(-2, -2)
        squareTextView.layoutParams = layoutParams
        squareTextView.id = com.google.maps.android.R.id.amu_text
        val twelveDpi = (12.0f * this.density).toInt()
        squareTextView.setPadding(twelveDpi, twelveDpi, twelveDpi, twelveDpi)
        return squareTextView
    }


    /*
     * Return Resource Id of icon to use to display current cluster.
     */
    private fun getClusterIcon(type: ReportType, count: Int) : Int {
        val iconIds = when(type) {
            ReportType.GASHT -> gashtIcons
            ReportType.VAN -> vanIcons
            ReportType.STOP -> stopIcons
            ReportType.UNSPECIFIED -> unspecifiedIcons
        }
        return if (count < iconIds.size) iconIds[count - 1] else iconIds[iconIds.size - 1]
    }

    /*
     * Obtain resource array of icon ids.
     */
    private fun getDrawableArray(type: ReportType): IntArray {
        val arr: TypedArray = when(type) {
            ReportType.VAN -> {
                context.resources.obtainTypedArray(R.array.array_van_icons)
            }
            ReportType.GASHT -> {
                context.resources.obtainTypedArray(R.array.array_gasht_icons)
            }
            ReportType.STOP -> {
                context.resources.obtainTypedArray(R.array.array_stop_icons)
            }
            ReportType.UNSPECIFIED -> {
                context.resources.obtainTypedArray(R.array.array_unspecified_icons)
            }
        }
        val array = IntArray(arr.length(), { i -> arr.getResourceId(i, 0) })
        arr.recycle()
        return array
    }

    private fun convertNumbers(str: String): String {
        var chars = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val builder = StringBuilder()
        for (i in 0 until str.length) {
            if (Character.isDigit(str[i])) {
                builder.append(chars[str[i].toInt() - 48])
            } else {
                builder.append(str[i])
            }
        }
        return builder.toString()
    }
}
