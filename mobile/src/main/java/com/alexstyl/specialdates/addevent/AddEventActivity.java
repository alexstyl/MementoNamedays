package com.alexstyl.specialdates.addevent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.alexstyl.android.AndroidDateLabelCreator;
import com.alexstyl.specialdates.FilePathProvider;
import com.alexstyl.specialdates.Optional;
import com.alexstyl.specialdates.R;
import com.alexstyl.specialdates.addevent.ui.AvatarCameraButtonView;
import com.alexstyl.specialdates.addevent.ui.ContactSuggestionView;
import com.alexstyl.specialdates.android.AndroidStringResources;
import com.alexstyl.specialdates.date.Date;
import com.alexstyl.specialdates.events.peopleevents.EventType;
import com.alexstyl.specialdates.images.ImageLoader;
import com.alexstyl.specialdates.service.PeopleEventsProvider;
import com.alexstyl.specialdates.ui.base.ThemedActivity;
import com.alexstyl.specialdates.ui.widget.MementoToolbar;
import com.novoda.notils.caster.Views;

public class AddEventActivity extends ThemedActivity implements PictureOptionSelectDialog.OnPictureOptionSelectedListener {

    private static final int CODE_PICTURE_TAKE = 501;
    private static final int CODE_PICK_A_FILE = 502;

    private AddEventsPresenter presenter;
    private PictureTakeRequest pictureTakeRequest;
    private FilePicker filePicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.slide_in_from_below, R.anim.stay);
        setContentView(R.layout.activity_add_event);
        pictureTakeRequest = new PictureTakeRequest(getPackageManager(), new FilePathProvider(getApplication()), getApplicationContext());
        filePicker = new FilePicker();
        // TODO analytics
        // TODO black and white icons for X
        ImageLoader imageLoader = ImageLoader.createSquareThumbnailLoader(getResources());

        MementoToolbar toolbar = Views.findById(this, R.id.memento_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_white);
        AvatarCameraButtonView avatarView = Views.findById(this, R.id.add_event_avatar);
        ContactSuggestionView contactSuggestionView = Views.findById(this, R.id.add_event_contact_autocomplete);
        RecyclerView eventsView = Views.findById(this, R.id.add_event_events);
        eventsView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        eventsView.setHasFixedSize(true);
        ContactEventsAdapter adapter = new ContactEventsAdapter(onEventTappedListener);
        eventsView.setAdapter(adapter);

        PeopleEventsProvider peopleEventsProvider = PeopleEventsProvider.newInstance(this);
        ContactEventViewModelFactory factory = new ContactEventViewModelFactory(new AndroidDateLabelCreator(this));
        AndroidStringResources stringResources = new AndroidStringResources(getResources());
        AddEventViewModelFactory newEventFactory = new AddEventViewModelFactory(stringResources);
        ContactEventsFetcher contactEventsFetcher = new ContactEventsFetcher(
                getSupportLoaderManager(),
                this,
                peopleEventsProvider,
                factory,
                newEventFactory
        );

        WriteableAccountsProvider accountsProvider = WriteableAccountsProvider.from(this);
        ContactEventPersister contactEventPersister = new ContactEventPersister(getContentResolver(), accountsProvider, peopleEventsProvider);
        MessageDisplayer messageDisplayer = new MessageDisplayer(getApplicationContext());
        presenter = new AddEventsPresenter(
                imageLoader,
                contactEventsFetcher,
                adapter,
                contactSuggestionView,
                avatarView,
                toolbar,
                factory,
                new AddEventViewModelFactory(stringResources),
                contactEventPersister,
                messageDisplayer
        );
        presenter.startPresenting(new OnCameraClickedListener() {
            @Override
            public void onPictureRetakenRequested() {
                PictureOptionSelectDialog.withRemoveOption().show(getSupportFragmentManager(), "picture_option_select");
            }

            @Override
            public void onNewPictureTakenRequested() {
                PictureOptionSelectDialog.withoutRemoveOption().show(getSupportFragmentManager(), "picture_option_select");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_PICTURE_TAKE && resultCode == RESULT_OK) {
            Uri imageUri = pictureTakeRequest.getCurrentPhotoPath();
            presenter.presentAvatar(imageUri);
            pictureTakeRequest.finishRequest();
        } else if (requestCode == CODE_PICK_A_FILE && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            presenter.presentAvatar(imageUri);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_event, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                cancelActivity();
                break;
            case R.id.menu_add_event_save:
                presenter.saveChanges();
                finishActivitySuccessfully();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void finishActivitySuccessfully() {
        setResult(RESULT_OK);
        finish();
    }

    protected void cancelActivity() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void finish() {
        overridePendingTransition(0, R.anim.slide_out_from_below);
        super.finish();
    }

    private final OnEventTappedListener onEventTappedListener = new OnEventTappedListener() {
        @Override
        public void onEventTapped(ContactEventViewModel viewModel) {
            final EventType eventType = viewModel.getEventType();
            final Optional<Date> initialDate = viewModel.getDate();
            EventDatePickerDialogFragment dialog = EventDatePickerDialogFragment.newInstance(eventType, initialDate, new EventDatePickerDialogFragment.OnBirthdaySelectedListener() {
                @Override
                public void onDatePicked(Date date) {
                    if (hasSelectedNewDate(date)) {
                        presenter.onEventDatePicked(eventType, date);
                    }
                }

                private boolean hasSelectedNewDate(Date date) {
                    return !initialDate.isPresent() || !initialDate.get().equals(date);
                }
            });
            dialog.show(getSupportFragmentManager(), "pick_event");
        }

        @Override
        public void onEventRemoved(EventType eventType) {
            presenter.onEventRemoved(eventType);
        }
    };

    @Override
    public void onOptionSelected(PictureSelectOption option) {
        switch (option) {
            case TAKE_PICTURE:
                pictureTakeRequest.takeNewPicture(this, CODE_PICTURE_TAKE);
                break;
            case PICK_EXISTING:
                filePicker.pickAFile(this, CODE_PICK_A_FILE);
                break;
            case REMOVE:
                presenter.removeAvatar();
                break;
            default:
                break;
        }
    }
}