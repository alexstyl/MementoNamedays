package com.alexstyl.specialdates;

import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;

import com.alexstyl.android.AlarmManagerCompat;
import com.alexstyl.resources.ResourcesModule;
import com.alexstyl.specialdates.dailyreminder.DailyReminderPreferences;
import com.alexstyl.specialdates.dailyreminder.DailyReminderScheduler;
import com.alexstyl.specialdates.events.ContactsObserver;
import com.alexstyl.specialdates.events.PeopleEventsMonitor;
import com.alexstyl.specialdates.events.PreferenceChangedEventsUpdateTrigger;
import com.alexstyl.specialdates.events.namedays.activity.NamedaysInADayModule;
import com.alexstyl.specialdates.events.peopleevents.EventPreferences;
import com.alexstyl.specialdates.events.peopleevents.PeopleEventsModule;
import com.alexstyl.specialdates.facebook.FacebookPreferences;
import com.alexstyl.specialdates.facebook.friendimport.FacebookFriendsScheduler;
import com.alexstyl.specialdates.images.AndroidContactsImageDownloader;
import com.alexstyl.specialdates.images.ImageModule;
import com.alexstyl.specialdates.images.NutraBaseImageDecoder;
import com.alexstyl.specialdates.permissions.PermissionChecker;
import com.alexstyl.specialdates.ui.widget.ViewModule;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.utils.L;
import com.novoda.notils.logger.simple.Log;

import javax.inject.Inject;
import java.util.concurrent.Callable;

import net.danlew.android.joda.JodaTimeAndroid;

import io.reactivex.Observable;

import static java.util.Arrays.asList;

public class MementoApplication extends Application {

    private AppComponent appComponent;

    @Inject PeopleEventsMonitor eventsMonitor;
    @Inject EventPreferences eventPreferences;
    @Inject PermissionChecker contactPermissions;

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent =
                DaggerAppComponent.builder()
                        .appModule(new AppModule(this))
                        .resourcesModule(new ResourcesModule(getResources()))
                        .imageModule(new ImageModule(getResources()))
                        .peopleEventsModule(new PeopleEventsModule(this))
                        .viewModule(new ViewModule(getResources()))
                        .namedaysInADayModule(new NamedaysInADayModule())
                        .build();

        appComponent.inject(this);

        initialiseDependencies();
        ErrorTracker.startTracking(this);

        DailyReminderPreferences preferences = DailyReminderPreferences.newInstance(this);
        if (preferences.isEnabled()) {
            AlarmManagerCompat alarmManager = AlarmManagerCompat.from(this);
            new DailyReminderScheduler(alarmManager, this).setupReminder(preferences);
        }
        if (FacebookPreferences.newInstance(this).isLoggedIn()) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            new FacebookFriendsScheduler(this, alarmManager).scheduleNext();
        }

        setupDatabaseRefresher();
    }

    PreferenceChangedEventsUpdateTrigger preferenceChangedEventsUpdateTrigger;

    private void setupDatabaseRefresher() {
        preferenceChangedEventsUpdateTrigger = new PreferenceChangedEventsUpdateTrigger(
                EasyPreferences.createForDefaultPreferences(this),
                getResources(),
                R.string.key_enable_namedays,
                R.string.key_nameday_lang,
                R.string.key_namedays_full_name
        );

        eventsMonitor.startMonitoring(
                asList(
                        new ContactsObserver(getContentResolver()),
                        preferenceChangedEventsUpdateTrigger
                ));

        boolean eventsHaveBeenInitialised = eventPreferences.hasBeenInitialised();
        if (!eventsHaveBeenInitialised && contactPermissions.canReadAndWriteContacts()) {
            // if we don't have contact permission, we'll update it is granted
            Observable.fromCallable(new Callable<Integer>() {
                @Override
                public Integer call() {
                    eventsMonitor.updateEvents();
                    eventPreferences.markEventsAsInitialised();
                    return 5;
                }
            })
                    .subscribe();
        }
    }

    protected void initialiseDependencies() {
        Log.setShowLogs(BuildConfig.DEBUG);
        JodaTimeAndroid.init(this);
        initImageLoader(this);
    }

    @SuppressWarnings("MagicNumber")
    public static void initImageLoader(Context context) {
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.MIN_PRIORITY)
                .threadPoolSize(10)
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .imageDecoder(new NutraBaseImageDecoder(BuildConfig.DEBUG))
                .imageDownloader(new AndroidContactsImageDownloader(context));
        L.writeLogs(BuildConfig.DEBUG);
        com.nostra13.universalimageloader.core.ImageLoader.getInstance().init(config.build());
    }

    public AppComponent getApplicationModule() {
        return appComponent;
    }
}
