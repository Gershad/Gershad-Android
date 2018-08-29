package com.gershad.gershad.onboarding

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gershad.gershad.R
import kotlinx.android.synthetic.main.fragment_onboard.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OnboardingFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [OnboardingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OnboardingFragment : Fragment() {

    private var mDrawableId: Int? = null
    private var mTitleId: Int? = null
    private var mTextId: Int? = null

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mDrawableId = arguments!!.getInt(ARG_DRAWABLE_ID)
            mTextId = arguments!!.getInt(ARG_TEXT)
            mTitleId = arguments!!.getInt(ARG_TITLE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onboard_image.setImageDrawable(ContextCompat.getDrawable(context!!, mDrawableId ?: R.drawable.onboarding1))
        onboard_textview.text = context!!.getString(mTextId!!)
        onboard_textview_title?.text = context!!.getString(mTitleId!!)
    }


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        private const val ARG_DRAWABLE_ID = "argDrawableId"
        private const val ARG_TEXT = "argText"
        private const val ARG_TITLE = "argTitle"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.

         * @param drawableId Image ID.
         * *
         * @param text Display Text.
         * *
         * @return A new instance of fragment OnboardFragment.
         */
        fun newInstance(drawableId: Int, title: Int, text: Int): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle()
            args.putInt(ARG_DRAWABLE_ID, drawableId)
            args.putInt(ARG_TITLE, title)
            args.putInt(ARG_TEXT, text)
            fragment.arguments = args
            return fragment
        }
    }
}
