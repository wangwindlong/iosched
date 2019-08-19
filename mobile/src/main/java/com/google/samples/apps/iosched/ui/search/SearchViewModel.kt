/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.di.SearchUsingRoomEnabledFlag
import com.google.samples.apps.iosched.shared.domain.search.SearchDbUseCase
import com.google.samples.apps.iosched.shared.domain.search.SearchUseCase
import com.google.samples.apps.iosched.shared.domain.search.Searchable
import com.google.samples.apps.iosched.shared.domain.search.Searchable.SearchedCodelab
import com.google.samples.apps.iosched.shared.domain.search.Searchable.SearchedSession
import com.google.samples.apps.iosched.shared.domain.search.Searchable.SearchedSpeaker
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.ui.search.SearchResultType.CODELAB
import com.google.samples.apps.iosched.ui.search.SearchResultType.SESSION
import com.google.samples.apps.iosched.ui.search.SearchResultType.SPEAKER
import timber.log.Timber
import javax.inject.Inject

class SearchViewModel @Inject constructor(
    private val analyticsHelper: AnalyticsHelper,
    private val loadSearchResultsUseCase: SearchUseCase,
    private val loadDbSearchResultsUseCase: SearchDbUseCase
) : ViewModel(), SearchResultActionHandler {

    @Inject
    @JvmField
    @SearchUsingRoomEnabledFlag
    var searchUsingRoomFeatureEnabled: Boolean = false

    private val _navigateToSessionAction = MutableLiveData<Event<SessionId>>()
    val navigateToSessionAction: LiveData<Event<SessionId>> = _navigateToSessionAction

    private val _navigateToSpeakerAction = MutableLiveData<Event<SpeakerId>>()
    val navigateToSpeakerAction: LiveData<Event<SpeakerId>> = _navigateToSpeakerAction

    // Has codelabUrl as String
    private val _navigateToCodelabAction = MutableLiveData<Event<String>>()
    val navigateToCodelabAction: LiveData<Event<String>> = _navigateToCodelabAction

    private val loadSearchResults = MutableLiveData<Result<List<Searchable>>>()

    private val _searchResults = MediatorLiveData<List<SearchResult>>()
    val searchResults: LiveData<List<SearchResult>> = _searchResults

    private val _isEmpty = MediatorLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    init {
        _searchResults.addSource(loadSearchResults) {
            val result = (it as? Result.Success)?.data ?: emptyList()
            _searchResults.value = result.map { searched ->
                when (searched) {
                    is SearchedSession -> {
                        val session = searched.session
                        SearchResult(
                            session.title,
                            session.type.displayName,
                            SESSION,
                            session.id
                        )
                    }
                    is SearchedSpeaker -> {
                        val speaker = searched.speaker
                        SearchResult(
                            speaker.name,
                            "Speaker",
                            SPEAKER,
                            speaker.id
                        )
                    }
                    is SearchedCodelab -> {
                        val codelab = searched.codelab
                        SearchResult(
                            codelab.title,
                            "Codelab",
                            CODELAB,
                            // This may not be unique, but to navigate to a meaningful page,
                            // assigning codelabUrl as id
                            codelab.codelabUrl
                        )
                    }
                }
            }
        }
        _isEmpty.addSource(loadSearchResults) {
            _isEmpty.value = it.successOr(null).isNullOrEmpty()
        }
    }

    override fun openSearchResult(searchResult: SearchResult) {
        when (searchResult.type) {
            SESSION -> {
                val sessionId = searchResult.objectId
                analyticsHelper.logUiEvent("Session: $sessionId",
                    AnalyticsActions.SEARCH_RESULT_CLICK)
                _navigateToSessionAction.value = Event(sessionId)
            }
            SPEAKER -> {
                val speakerId = searchResult.objectId
                analyticsHelper.logUiEvent("Speaker: $speakerId",
                    AnalyticsActions.SEARCH_RESULT_CLICK)
                _navigateToSpeakerAction.value = Event(speakerId)
            }
            CODELAB -> {
                val url = searchResult.objectId
                analyticsHelper.logUiEvent("Codelab: $url",
                    AnalyticsActions.SEARCH_RESULT_CLICK)
                _navigateToCodelabAction.value = Event(url)
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        if (newQuery.length < 2) {
            onQueryCleared()
            return
        }
        analyticsHelper.logUiEvent("Query: $newQuery", AnalyticsActions.SEARCH_QUERY_SUBMIT)
        executeSearch(newQuery)
    }

    private fun executeSearch(query: String) {
        if (searchUsingRoomFeatureEnabled) {
            Timber.d("Searching for query using Room: $query")
            loadDbSearchResultsUseCase(query, loadSearchResults)
        } else {
            Timber.d("Searching for query without using Room: $query")
            loadSearchResultsUseCase(query, loadSearchResults)
        }
    }

    private fun onQueryCleared() {
        _searchResults.value = emptyList()
        // Explicitly set false to not show the "No results" state
        _isEmpty.value = false
    }
}
