package com.alexstyl.specialdates.upcoming

import android.view.View
import android.widget.TextView
import com.alexstyl.specialdates.upcoming.view.OnUpcomingEventClickedListener

class NamedaysViewHolder(view: View, private val namedays: TextView)
    : UpcomingRowViewHolder<UpcomingNamedaysViewModel>(view) {


    override fun bind(viewModelUpcoming: UpcomingNamedaysViewModel, listener: OnUpcomingEventClickedListener) {
        namedays.text = viewModelUpcoming.namesLabel
        itemView.setOnClickListener { listener.onNamedayClicked(viewModelUpcoming.date) }
    }


}
