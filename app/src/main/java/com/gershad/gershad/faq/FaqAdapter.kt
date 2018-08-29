package com.gershad.gershad.faq

import android.content.Context
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import com.gershad.gershad.R
import kotlinx.android.synthetic.main.list_group.view.*
import kotlinx.android.synthetic.main.list_item.view.*


class FaqAdapter(private val context: Context, private val questions: Array<Int>, private val answers: Array<Int>, private val icons: Array<Int>) : BaseExpandableListAdapter() {

    override fun isChildSelectable(p0: Int, p1: Int): Boolean {
        return false
    }

    override fun getGroupView(groupId: Int, b: Boolean, view: View?, viewGroup: ViewGroup?): View {

        val headerContent = getGroup(groupId) as Pair<Int, Int>

        val groupView : View = view ?: LayoutInflater.from(context).inflate(R.layout.list_group, null)

        groupView.list_header.text = context.getString(headerContent.first)
        groupView.list_header_icon.setImageResource(headerContent.second)

        return groupView
    }

    override fun getChildrenCount(p0: Int): Int {
        return 1
    }

    override fun getChild(groupId: Int, childId: Int): Any {
        return context.getString(answers[groupId])
    }

    override fun getGroupId(groupId: Int): Long {
        return groupId.toLong()
    }

    override fun getChildView(groupId: Int, childId: Int, b: Boolean, view: View?, viewGroup: ViewGroup?): View {
        val childText = getChild(groupId, childId) as String

        val groupView : View = view ?: LayoutInflater.from(context).inflate(R.layout.list_item, null)

        groupView.list_item.text = Html.fromHtml(childText)
        groupView.list_item.movementMethod = LinkMovementMethod.getInstance()

        return groupView
    }

    override fun getChildId(groupId: Int, childId: Int): Long {
        return childId.toLong()
    }

    override fun getGroupCount(): Int {
        return questions.size
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroup(groupId: Int): Any {
        return Pair(questions[groupId], icons[groupId])
    }

}