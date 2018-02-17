package com.alexstyl.specialdates.events.peopleevents

import com.alexstyl.specialdates.date.ContactEvent

class PeopleEventsStaticEventsRefresher(
        private val repository: AndroidEventsRepository,
        private val persister: AndroidPeopleEventsPersister) {

    fun rebuildEvents() {
        persister.deleteAllDeviceEvents()
        val contacts = repository.fetchPeopleWithEvents()
        storeContactsToProvider(contacts)
    }

    private fun storeContactsToProvider(contacts: List<ContactEvent>) {
        persister.insertAnnualEvents(contacts)
    }

}
