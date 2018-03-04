package com.alexstyl.specialdates

import com.alexstyl.specialdates.events.peopleevents.PeopleEventsUpdater
import com.alexstyl.specialdates.events.peopleevents.UpcomingEventsViewRefresher
import com.alexstyl.specialdates.upcoming.PeopleEventsRefreshJob
import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

class UpcomingEventsJobCreator(private val peopleEventsUpdater: PeopleEventsUpdater,
                               private val uiRefresher: UpcomingEventsViewRefresher) : JobCreator {

    override fun create(tag: String): Job? {
        return PeopleEventsRefreshJob(peopleEventsUpdater, uiRefresher)
    }
}
