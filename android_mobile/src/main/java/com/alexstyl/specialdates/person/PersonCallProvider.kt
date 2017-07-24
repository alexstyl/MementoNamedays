package com.alexstyl.specialdates.person

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Intent
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.alexstyl.specialdates.ErrorTracker
import com.alexstyl.specialdates.R
import com.alexstyl.specialdates.contact.Contact
import com.alexstyl.specialdates.contact.ContactSource
import com.alexstyl.specialdates.entity.Phone
import io.reactivex.Observable

class PersonCallProvider(val resources: Resources, val contentResolver: ContentResolver) {

    fun getCallsFor(contact: Contact): Observable<List<ContactCallViewModel>> {
        return Observable.fromCallable {
            val list = ArrayList<ContactCallViewModel>()
            if (contact.source == ContactSource.SOURCE_FACEBOOK) {
                list.add(facebookCallsFor(contact))
            } else {
                phoneNumbers()
                contentResolver.query()
                list.add(ContactCallViewModel("Messenger", contact.displayName.toString(), resources.getDrawable(R.drawable.ic_facebook_messenger), Intent()))
            }
            list
        }
        // Find phone numbers of contact
        // whatsapp
        // skype
        // facebook messenger
    }

    private fun phoneNumbers() {
        val phones = java.util.ArrayList<Phone>()
        val phoneCursor: Cursor?
        val selectionArgs = arrayOf<String>(contactID.toString())

        val contentUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

        phoneCursor = resolver.query(contentUri, DataTypeQuery.PROJECTION,
                DataTypeQuery.SELECTION,
                selectionArgs, DataTypeQuery.SORTORDER
        )

        if (phoneCursor == null) {
            return phones
        }

        try {
            val colData1 = phoneCursor!!.getColumnIndex(ContactsContract.Data.DATA1)
            val colData2 = phoneCursor.getColumnIndex(ContactsContract.Data.DATA2)
            val colData3 = phoneCursor.getColumnIndex(ContactsContract.Data.DATA3)

            while (phoneCursor.moveToNext()) {
                val number = phoneCursor.getString(colData1)
                val type = phoneCursor.getInt(colData2)
                val label = phoneCursor.getString(colData3)

                val data = Phone(number, type, label)
                if (!phones.contains(data)) {
                    phones.add(data)
                }
            }

        } catch (e: Exception) {
            ErrorTracker.track(e)
        } finally {
            if (phoneCursor != null && !phoneCursor!!.isClosed) {
                phoneCursor!!.close()
            }
        }

        return phones
    }

    private fun facebookCallsFor(contact: Contact): ContactCallViewModel {
        val uri = ContentUris.withAppendedId(Uri.parse("fb-messenger://user/"), contact.contactID)
        val startIntent = Intent(Intent.ACTION_VIEW, uri)
        return ContactCallViewModel("Messenger", contact.displayName.toString(), resources.getDrawable(R.drawable.ic_facebook_messenger), startIntent)
    }
}