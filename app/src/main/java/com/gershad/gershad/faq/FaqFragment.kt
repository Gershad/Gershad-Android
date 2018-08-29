package com.gershad.gershad.faq

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gershad.gershad.R
import kotlinx.android.synthetic.main.fragment_faq.*


class FaqFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_faq, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = FaqAdapter(context!!, GUIDE_TITLES, GUIDE_CONTENT, GUIDE_ICON)
        expandable_list.setAdapter(adapter)
    }

    companion object {
        fun newInstance() = FaqFragment()

        private val GUIDE_TITLES = arrayOf(R.string.yellow_location_pin_title, R.string.grey_location_pin_title, R.string.checkpoint_title, R.string.van_title, R.string.gasht_alpha_full_title, R.string.gasht_alpha_half_title, R.string.confirmed_title, R.string.comment_title, R.string.favorite_title, R.string.share_title, R.string.report_list_title, R.string.feedback_title, R.string.settings_title, R.string.delete_title)

        private val GUIDE_CONTENT = arrayOf(R.string.yellow_location_pin_content, R.string.grey_location_pin_content, R.string.checkpoint_content, R.string.van_content, R.string.gasht_alpha_full_content, R.string.gasht_alpha_half_content, R.string.confirmed_content, R.string.comment_content, R.string.favorite_content, R.string.share_content, R.string.report_list_content, R.string.feedback_content, R.string.settings_content, R.string.delete_content)

        private val GUIDE_ICON = arrayOf(R.drawable.guide_yellow_location_pin, R.drawable.guide_grey_location_pin, R.drawable.guide_checkpoint, R.drawable.guide_van, R.drawable.guide_gasht_alpha_full, R.drawable.guide_gasht_alpha_half, R.drawable.guide_confirmed, R.drawable.guide_comment, R.drawable.guide_favorite, R.drawable.guide_share, R.drawable.guide_report_list, R.drawable.guide_feedback, R.drawable.guide_settings, R.drawable.guide_delete)
    }
}