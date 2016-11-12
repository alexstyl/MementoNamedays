package com.alexstyl.specialdates.service;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.alexstyl.specialdates.ErrorTracker;
import com.alexstyl.specialdates.contact.Contact;
import com.alexstyl.specialdates.contact.ContactNotFoundException;
import com.alexstyl.specialdates.contact.ContactProvider;
import com.alexstyl.specialdates.date.ContactEvent;
import com.alexstyl.specialdates.date.Date;
import com.alexstyl.specialdates.datedetails.PeopleEventsQuery;
import com.alexstyl.specialdates.events.peopleevents.ContactEvents;
import com.alexstyl.specialdates.events.peopleevents.EventType;
import com.alexstyl.specialdates.events.database.PeopleEventsContract;
import com.alexstyl.specialdates.events.namedays.NamedayPreferences;
import com.alexstyl.specialdates.upcoming.TimePeriod;
import com.novoda.notils.exception.DeveloperError;
import com.novoda.notils.logger.simple.Log;

import java.util.ArrayList;
import java.util.List;

public class PeopleEventsProvider {

    private final ContactProvider contactProvider;
    private final ContentResolver resolver;
    private final NamedayPreferences namedayPreferences;

    private static final String[] PEOPLE_PROJECTION = new String[]{PeopleEventsContract.PeopleEvents.DATE};

    public static PeopleEventsProvider newInstance(Context context) {
        ContactProvider provider = ContactProvider.get(context);
        ContentResolver resolver = context.getContentResolver();
        NamedayPreferences namedayPreferences = NamedayPreferences.newInstance(context);
        return new PeopleEventsProvider(provider, resolver, namedayPreferences);
    }

    public PeopleEventsProvider(ContactProvider contactProvider, ContentResolver resolver, NamedayPreferences namedayPreferences) {
        this.contactProvider = contactProvider;
        this.resolver = resolver;
        this.namedayPreferences = namedayPreferences;
    }

    public ContactEvents getCelebrationsClosestTo(Date date) {
        Date closestDate = findClosestDateTo(date);
        return getCelebrationDateFor(closestDate);
    }

    public ContactEvents getCelebrationDateFor(Date date) {
        List<ContactEvent> contactEvents = new ArrayList<>();
        Cursor cursor = resolver.query(
                PeopleEventsContract.PeopleEvents.CONTENT_URI,
                null,
                getSelection(),
                getSelectArgs(date),
                PeopleEventsContract.PeopleEvents.CONTACT_ID
        );
        if (isInvalid(cursor)) {
            throw new DeveloperError("Cursor was invalid");
        }

        while (cursor.moveToNext()) {
            long contactId = PeopleEventsContract.PeopleEvents.getContactIdFrom(cursor);
            try {
                Contact contact = contactProvider.getOrCreateContact(contactId);
                EventType eventType = PeopleEventsContract.PeopleEvents.getEventType(cursor);

                ContactEvent event = new ContactEvent(eventType, date, contact);
                contactEvents.add(event);
            } catch (Exception e) {
                ErrorTracker.track(e);
            }
        }
        cursor.close();
        return ContactEvents.createFrom(date, contactEvents);
    }

    private Date findClosestDateTo(Date date) {
        Cursor cursor = queryDateClosestTo(date);
        if (isInvalid(cursor)) {
            throw new DeveloperError("Cursor was invalid");
        }

        Date dateFrom;
        if (cursor.moveToFirst()) {
            dateFrom = PeopleEventsContract.PeopleEvents.getDateFrom(cursor);
        } else {
            dateFrom = date;
        }
        cursor.close();
        return dateFrom;
    }

    private static final Uri PEOPLE_EVENTS = PeopleEventsContract.PeopleEvents.CONTENT_URI;

    private Cursor queryDateClosestTo(Date date) {
        return resolver.query(
                PEOPLE_EVENTS,
                PEOPLE_PROJECTION, whereDateIsEqualOrAfter(), thePassing(date), onlyTheFirstMatch()
        );
    }

    @NonNull
    private String onlyTheFirstMatch() {
        return PeopleEventsContract.PeopleEvents.DATE + " ASC" + " LIMIT 1";
    }

    private String[] thePassing(Date date) {
        return new String[]{
                date.toShortDate()
        };
    }

    private String whereDateIsEqualOrAfter() {
        return PeopleEventsContract.PeopleEvents.DATE + " >= ?";
    }

    private boolean isInvalid(Cursor cursor) {
        return cursor == null || cursor.isClosed();
    }

    private String[] getSelectArgs(Date date) {
        return new String[]{date.toShortDate()};
    }

    private String getSelection() {
        if (namedaysAreEnabled()) {
            return PeopleEventsQuery.Date.SELECT;
        } else {
            return PeopleEventsQuery.Date.SELECT_ONLY_BIRTHDAYS;
        }
    }

    private boolean namedaysAreEnabled() {
        return namedayPreferences.isEnabled();
    }

    public List<ContactEvent> getCelebrationDateFor(TimePeriod timeDuration) {
        List<ContactEvent> contactEvents = new ArrayList<>();
        Cursor cursor = queryPeopleEvents(timeDuration.getFrom(), timeDuration.getTo());
        throwIfInvalid(cursor);

        while (cursor.moveToNext()) {
            try {
                ContactEvent contactEvent = getContactEventFrom(cursor);
                contactEvents.add(contactEvent);
            } catch (ContactNotFoundException e) {
                Log.w(e);
            }
        }

        cursor.close();
        return contactEvents;
    }

    private Cursor queryPeopleEvents(Date startingDate, Date endingDate) {
        String select = PeopleEventsContract.PeopleEvents.DATE + " >= ? AND " + PeopleEventsContract.PeopleEvents.DATE + " <=?";
        String[] selectArgs = new String[]{
                startingDate.toShortDate(),
                endingDate.toShortDate()
        };

        Cursor cursor = resolver.query(
                PeopleEventsContract.PeopleEvents.CONTENT_URI,
                PROJECTION,
                select,
                selectArgs,
                PeopleEventsContract.PeopleEvents.DATE + " ASC"
        );
        if (isInvalid(cursor)) {
            ErrorTracker.track(new IllegalStateException("People Events returned invalid cursor"));
        }
        return cursor;
    }

    private static final String[] PROJECTION = {
            PeopleEventsContract.PeopleEvents.CONTACT_ID,
            PeopleEventsContract.PeopleEvents.SOURCE,

            PeopleEventsContract.PeopleEvents.DATE,

            PeopleEventsContract.PeopleEvents.EVENT_TYPE,
    };

    private ContactEvent getContactEventFrom(Cursor cursor) throws ContactNotFoundException {
        long contactId = PeopleEventsContract.PeopleEvents.getContactIdFrom(cursor);
        Contact contact = contactProvider.getOrCreateContact(contactId);
        Date date = PeopleEventsContract.PeopleEvents.getDateFrom(cursor);
        EventType eventType = PeopleEventsContract.PeopleEvents.getEventType(cursor);

        return new ContactEvent(eventType, date, contact);
    }

    private void throwIfInvalid(Cursor cursor) {
        if (isInvalid(cursor)) {
            throw new RuntimeException("Invalid cursor");
        }
    }

}
