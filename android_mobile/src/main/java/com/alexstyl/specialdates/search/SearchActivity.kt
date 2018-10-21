package com.alexstyl.specialdates.search

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View

import com.alexstyl.specialdates.MementoApplication
import com.alexstyl.specialdates.R
import com.alexstyl.specialdates.analytics.Analytics
import com.alexstyl.specialdates.contact.Contact
import com.alexstyl.specialdates.date.Date
import com.alexstyl.specialdates.date.DateLabelCreator
import com.alexstyl.specialdates.events.namedays.NamedayUserSettings
import com.alexstyl.specialdates.events.namedays.calendar.resource.NamedayCalendarProvider
import com.alexstyl.specialdates.images.ImageLoader
import com.alexstyl.specialdates.people.PeopleItemDecorator
import com.alexstyl.specialdates.ui.base.ThemedMementoActivity
import com.alexstyl.specialdates.ui.widget.SpacesItemDecoration
import javax.inject.Inject

class SearchActivity : ThemedMementoActivity() {

    @Inject lateinit var presenter: SearchPresenter
    private lateinit var searchResultView: SearchResultView

    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var labelCreator: DateLabelCreator
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var namedayUserSettings: NamedayUserSettings
    @Inject lateinit var namedayCalendarProvider: NamedayCalendarProvider


    val navigator by lazy {
        SearchNavigator(this, analytics)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val applicationModule = (application as MementoApplication).applicationModule
        applicationModule.inject(this)

        val searchbar = findViewById<SearchToolbar>(R.id.search_searchbar)

//        val content = findViewById<ViewGroup>(R.id.search_content)
        val resultView = findViewById<RecyclerView>(R.id.search_results).apply {
            layoutManager = LinearLayoutManager(this@SearchActivity, LinearLayoutManager.VERTICAL, false)
        }

        val resultsAdapter = SearchResultsAdapter(imageLoader, labelCreator, object : SearchResultClickListener {
            override fun onContactClicked(contact: Contact) {
                navigator.toContactDetails(contact)
            }

            override fun onNamedayClicked(date: Date) {
                navigator.toNamedays(date)
            }
        })
        resultView.adapter = resultsAdapter
        resultView.addItemDecoration(SpacesItemDecoration(resources.getDimensionPixelSize(R.dimen.search_result_vertical_padding)))

        val namesSuggestionsView = findViewById<RecyclerView>(R.id.search_nameday_suggestions)
        if (namedayUserSettings.isEnabled) {
            val namesAdapter = NameSuggestionsAdapter { name ->
                searchbar.setText(name)
            }
            namesSuggestionsView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            namesSuggestionsView.adapter = namesAdapter

            searchResultView = AndroidSearchResultView(resultsAdapter, searchbar, namesAdapter)
        } else {
            namesSuggestionsView.visibility = View.GONE
            searchResultView = AndroidSearchResultView(resultsAdapter, searchbar, null)
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.presentInto(searchResultView)
    }

    override fun onStop() {
        super.onStop()
        presenter.stopPresenting()
    }
}