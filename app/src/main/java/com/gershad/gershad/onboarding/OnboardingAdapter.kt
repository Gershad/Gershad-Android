package com.gershad.gershad.onboarding

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.gershad.gershad.R
import java.lang.IllegalArgumentException


class OnboardingAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment = OnboardingFragment.newInstance(withDrawable(position), withTitle(position), withText(position))

    override fun getCount(): Int = COUNT

    // note 'backwards' bc of viewpager
    private fun withDrawable(position: Int): Int {
        return when (position) {
            0 -> R.drawable.onboarding3
            1 -> R.drawable.onboarding2
            2 -> R.drawable.onboarding1
            else -> throw IllegalArgumentException("Missing icon for this position")
        }
    }

    // note 'backwards' bc of viewpager
    private fun withTitle(position: Int): Int {
        return when (position) {
            0 -> R.string.title_onboarding3
            1 -> R.string.title_onboarding2
            2 -> R.string.title_onboarding1
            else -> throw IllegalArgumentException("Missing text for this position")
        }
    }

    // note 'backwards' bc of viewpager
    private fun withText(position: Int): Int {
        return when (position) {
            0 -> R.string.text_activity_onboarding3
            1 -> R.string.text_activity_onboarding2
            2 -> R.string.text_activity_onboarding1
            else -> throw IllegalArgumentException("Missing text for this position")
        }
    }

    companion object {
        private const val COUNT: Int = 3
    }
}