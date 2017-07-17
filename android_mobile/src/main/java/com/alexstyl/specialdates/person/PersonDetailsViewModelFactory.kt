package com.alexstyl.specialdates.person

import com.alexstyl.specialdates.contact.Contact
import com.alexstyl.specialdates.date.ContactEvent
import com.alexstyl.specialdates.date.Date
import java.util.function.BiFunction

internal class PersonDetailsViewModelFactory : BiFunction<Contact, ContactEvent?, PersonDetailsViewModel> {

    override fun apply(contact: Contact, contactEvent: ContactEvent?): PersonDetailsViewModel {
        val ageAndStarSignBuilder = StringBuilder()
        if (contactEvent != null) {
            ageAndStarSignBuilder.append(ageOf(contactEvent.date!!))
            if (ageAndStarSignBuilder.isNotEmpty()) {
                ageAndStarSignBuilder.append(", ")
            }
            ageAndStarSignBuilder.append(starSignOf(contactEvent.date!!))
        }
        return PersonDetailsViewModel(contact.displayName.toString(), ageAndStarSignBuilder.toString(), contact.imagePath)
    }

    private fun ageOf(birthday: Date): String {
        if (birthday.hasYear()) {
            return 24.toString()
        }
        return ""
    }

    private fun starSignOf(birthday: Date): String {
        // TODO
        return "Sagittarius"
    }
}
