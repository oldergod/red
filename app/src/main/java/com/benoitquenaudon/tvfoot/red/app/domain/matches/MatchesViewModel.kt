package com.benoitquenaudon.tvfoot.red.app.domain.matches

import com.benoitquenaudon.tvfoot.red.app.common.schedulers.BaseSchedulerProvider
import com.benoitquenaudon.tvfoot.red.app.data.entity.FilterTeam
import com.benoitquenaudon.tvfoot.red.app.data.source.BaseMatchesRepository
import com.benoitquenaudon.tvfoot.red.app.data.source.BasePreferenceRepository
import com.benoitquenaudon.tvfoot.red.app.data.source.BaseTeamRepository
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.FilterAction.ClearFiltersAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.FilterAction.ClearSearchInputAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.FilterAction.LoadTagsAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.FilterAction.SearchInputAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.FilterAction.SearchInputAction.ClearSearchAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.FilterAction.SearchInputAction.SearchTeamAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.FilterAction.SearchedTeamSelectedAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.FilterAction.ToggleFilterBroadcasterAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.FilterAction.ToggleFilterCompetitionAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.FilterAction.ToggleFilterTeamAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.LoadNextPageAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.RefreshAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesAction.RefreshNotificationStatusAction
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.ClearFilters
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.ClearSearchInputIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.FilterInitialIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.SearchInputIntent.ClearSearchIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.SearchInputIntent.SearchTeamIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.SearchedTeamSelectedIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.ToggleFilterIntent.ToggleFilterBroadcasterIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.ToggleFilterIntent.ToggleFilterCompetitionIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.FilterIntent.ToggleFilterIntent.ToggleFilterTeamIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.InitialIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.LoadNextPageIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.RefreshIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesIntent.RefreshNotificationStatusIntent
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.ClearFiltersResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.ClearSearchInputResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.LoadTagsResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.SearchInputResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.SearchInputResult.ClearSearchResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.SearchInputResult.SearchTeamResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.SearchedTeamSelectedResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.SearchedTeamSelectedResult.TeamSearchFailure
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.SearchedTeamSelectedResult.TeamSearchInFlight
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.SearchedTeamSelectedResult.TeamSearchSuccess
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.ToggleFilterBroadcasterResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.ToggleFilterCompetitionResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.FilterResult.ToggleFilterTeamResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.LoadNextPageResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.RefreshNotificationStatusResult
import com.benoitquenaudon.tvfoot.red.app.domain.matches.MatchesResult.RefreshResult
import com.benoitquenaudon.tvfoot.red.app.mvi.RedViewModel
import com.benoitquenaudon.tvfoot.red.util.Quintuple
import com.benoitquenaudon.tvfoot.red.util.logAction
import com.benoitquenaudon.tvfoot.red.util.logIntent
import com.benoitquenaudon.tvfoot.red.util.logResult
import com.benoitquenaudon.tvfoot.red.util.logState
import com.benoitquenaudon.tvfoot.red.util.notOfType
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class MatchesViewModel @Inject constructor(
  private val intentsSubject: PublishSubject<MatchesIntent>,
  private val matchesRepository: BaseMatchesRepository,
  private val teamRepository: BaseTeamRepository,
  private val preferenceRepository: BasePreferenceRepository,
  private val schedulerProvider: BaseSchedulerProvider
) : RedViewModel<MatchesIntent, MatchesViewState>() {
  private val disposables = CompositeDisposable()

  private val statesObservable: Observable<MatchesViewState> by lazy(NONE) {
    compose().skip(1)
        .replay(1)
        .autoConnect(0)
  }

  override fun states(): Observable<MatchesViewState> = statesObservable

  override fun processIntents(intents: Observable<MatchesIntent>) {
    disposables.add(intents.subscribe(intentsSubject::onNext, intentsSubject::onError))
  }

  override fun onCleared() {
    disposables.dispose()
    super.onCleared()
  }

  private fun compose(): Observable<MatchesViewState> {
    return intentsSubject
        .publish { shared ->
          Observable.merge(
              shared.ofType(InitialIntent::class.java).take(1),
              shared.notOfType(InitialIntent::class.java)
          )
        }
        .publish { shared ->
          Observable.merge(
              shared.ofType(FilterInitialIntent::class.java).take(1),
              shared.notOfType(FilterInitialIntent::class.java)
          )
        }
        .doOnNext(this::logIntent)
        .map(this::actionFromIntent)
        .doOnNext(this::logAction)
        .compose<MatchesResult>(actionToResultTransformer)
        .doOnNext(this::logResult)
        .scan(MatchesViewState.idle(), MatchesViewStateMachine)
        .doOnNext(this::logState)
  }

  private fun actionFromIntent(intent: MatchesIntent): MatchesAction =
    when (intent) {
      InitialIntent -> RefreshAction
      RefreshIntent -> RefreshAction
      is LoadNextPageIntent -> LoadNextPageAction(intent.pageIndex)
      ClearFilters -> ClearFiltersAction
      is ToggleFilterCompetitionIntent -> ToggleFilterCompetitionAction(intent.tagName)
      is ToggleFilterBroadcasterIntent -> ToggleFilterBroadcasterAction(intent.tagName)
      is ToggleFilterTeamIntent -> ToggleFilterTeamAction(intent.teamCode)
      FilterInitialIntent -> LoadTagsAction
      is SearchTeamIntent -> SearchTeamAction(intent.input)
      ClearSearchIntent -> ClearSearchAction
      is SearchedTeamSelectedIntent -> SearchedTeamSelectedAction(intent.team)
      ClearSearchInputIntent -> ClearSearchInputAction
      is RefreshNotificationStatusIntent -> RefreshNotificationStatusAction(intent.matchId)
    }

  private val refreshTransformer: ObservableTransformer<RefreshAction, RefreshResult>
    get() = ObservableTransformer { actions: Observable<RefreshAction> ->
      actions.flatMap {
        matchesRepository.loadPageWithNotificationStatus(0)
            .toObservable()
            .map<RefreshResult> { RefreshResult.Success(it.first, it.second) }
            .onErrorReturn(RefreshResult::Failure)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .startWith(RefreshResult.InFlight)
      }
    }

  private val loadNextPageTransformer: ObservableTransformer<LoadNextPageAction, LoadNextPageResult>
    get() = ObservableTransformer { actions: Observable<LoadNextPageAction> ->
      actions.flatMap { (pageIndex) ->
        matchesRepository.loadPageWithNotificationStatus(pageIndex)
            .toObservable()
            .map<LoadNextPageResult> { (matches, notificationPairs) ->
              LoadNextPageResult.Success(pageIndex, matches, notificationPairs)
            }
            .onErrorReturn(LoadNextPageResult::Failure)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .startWith(LoadNextPageResult.InFlight)
      }
    }

  private val refreshNotificationStatusTransformer: ObservableTransformer<RefreshNotificationStatusAction, RefreshNotificationStatusResult>
    get() = ObservableTransformer { actions: Observable<RefreshNotificationStatusAction> ->
      actions
          .map(RefreshNotificationStatusAction::matchId)
          .flatMapSingle { matchId ->
            preferenceRepository
                .loadNotifyMatchStart(matchId)
                .map { RefreshNotificationStatusResult(matchId, it) }
          }
    }

  private val loadTagsTransformer: ObservableTransformer<LoadTagsAction, LoadTagsResult>
    get() = ObservableTransformer { actions: Observable<LoadTagsAction> ->
      actions.flatMap {
        Singles.zip(
            matchesRepository.loadTags(),
            preferenceRepository.loadFilteredCompetitionNames(),
            preferenceRepository.loadFilteredBroadcasterNames(),
            preferenceRepository.loadFilteredTeamCodes(),
            preferenceRepository.loadTeams(),
            ::Quintuple
        )
            .toObservable()
            .map<LoadTagsResult> {
              LoadTagsResult.Success(
                  tags = it.first,
                  filteredCompetitionNames = it.second,
                  filteredBroadcasterNames = it.third,
                  filteredTeamNames = it.fourth,
                  teams = it.fifth
              )
            }
            .onErrorReturn(LoadTagsResult::Failure)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .startWith(LoadTagsResult.InFlight)
      }
    }

  private val searchInputTransformer: ObservableTransformer<SearchInputAction, SearchInputResult>
    get() = ObservableTransformer { actions: Observable<SearchInputAction> ->
      actions.switchMap { action ->
        when (action) {
          is SearchTeamAction -> {
            if (action.input.length > 2) {
              teamRepository.findTeams(action.input)
                  .subscribeOn(schedulerProvider.io())
                  .delaySubscription(250, MILLISECONDS, schedulerProvider.ui())
                  .toObservable()
                  .map<SearchTeamResult> { teams -> SearchTeamResult.Success(action.input, teams) }
                  .onErrorReturn(SearchTeamResult::Failure)
                  .observeOn(schedulerProvider.ui())
                  .startWith(SearchTeamResult.InFlight(action.input))
            } else {
              Observable.just(SearchTeamResult.Success(action.input, emptyList()))
            }
          }
          ClearSearchAction -> Observable.just(ClearSearchResult)
        }
      }
    }

  private val clearFilterTransformer: ObservableTransformer<ClearFiltersAction, ClearFiltersResult>
    get() = ObservableTransformer { actions: Observable<ClearFiltersAction> ->
      actions.map {
        preferenceRepository.clearFilteredCompetitionNames()
        preferenceRepository.clearFilteredBroadcasterNames()
        preferenceRepository.clearFilteredTeamCodes()
        ClearFiltersResult
      }
    }

  private val toggleFilterCompetitionTransformer: ObservableTransformer<ToggleFilterCompetitionAction, ToggleFilterCompetitionResult>
    get() = ObservableTransformer { actions: Observable<ToggleFilterCompetitionAction> ->
      actions.map {
        preferenceRepository.toggleFilteredCompetitionName(it.tagName)
        ToggleFilterCompetitionResult(it.tagName)
      }
    }

  private val toggleFilterBroadcasterTransformer: ObservableTransformer<ToggleFilterBroadcasterAction, ToggleFilterBroadcasterResult>
    get() = ObservableTransformer { actions: Observable<ToggleFilterBroadcasterAction> ->
      actions.map {
        preferenceRepository.toggleFilteredBroadcasterName(it.tagName)
        ToggleFilterBroadcasterResult(it.tagName)
      }
    }

  private val toggleFilterTeamTransformer: ObservableTransformer<ToggleFilterTeamAction, ToggleFilterTeamResult>
    get() = ObservableTransformer { actions: Observable<ToggleFilterTeamAction> ->
      actions.map {
        preferenceRepository.toggleFilteredTeamCode(it.teamCode)
        ToggleFilterTeamResult(it.teamCode)
      }
    }

  private val clearSearchInputTransformer: ObservableTransformer<ClearSearchInputAction, ClearSearchInputResult>
    get() = ObservableTransformer { actions: Observable<ClearSearchInputAction> ->
      actions.map { ClearSearchInputResult }
    }

  private val searchedTeamSelectedTransformer: ObservableTransformer<SearchedTeamSelectedAction, SearchedTeamSelectedResult>
    get() = ObservableTransformer { actions: Observable<SearchedTeamSelectedAction> ->
      actions.flatMap { action ->
        preferenceRepository.addTeam(FilterTeam(action.team))
            .flatMap { preferenceRepository.toggleFilteredTeamCode(action.team.code) }
            .flatMap { matchesRepository.searchTeamMatchesWithNotificationStatus(action.team.code) }
            .toObservable()
            .map<SearchedTeamSelectedResult> { TeamSearchSuccess(it.first, it.second) }
            .onErrorReturn(::TeamSearchFailure)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .startWith(TeamSearchInFlight(action.team))
      }
    }

  private val actionToResultTransformer: ObservableTransformer<MatchesAction, MatchesResult>
    get() = ObservableTransformer { actions: Observable<MatchesAction> ->
      actions.publish { shared ->
        Observable.merge<MatchesResult>(
            shared.ofType(RefreshAction::class.java).compose(refreshTransformer),
            shared.ofType(LoadNextPageAction::class.java).compose(loadNextPageTransformer),
            shared.ofType(RefreshNotificationStatusAction::class.java)
                .compose(refreshNotificationStatusTransformer)
        )
            .mergeWith(
                Observable.merge<MatchesResult>(
                    shared.ofType(ToggleFilterBroadcasterAction::class.java)
                        .compose(toggleFilterBroadcasterTransformer),
                    shared.ofType(ToggleFilterCompetitionAction::class.java)
                        .compose(toggleFilterCompetitionTransformer),
                    shared.ofType(ToggleFilterTeamAction::class.java)
                        .compose(toggleFilterTeamTransformer),
                    shared.ofType(LoadTagsAction::class.java).compose(loadTagsTransformer)
                )
            )
            .mergeWith(
                Observable.merge<MatchesResult>(
                    shared.ofType(ClearFiltersAction::class.java).compose(clearFilterTransformer),
                    shared.ofType(SearchInputAction::class.java).compose(searchInputTransformer),
                    shared.ofType(SearchedTeamSelectedAction::class.java)
                        .compose(searchedTeamSelectedTransformer),
                    shared.ofType(ClearSearchInputAction::class.java).compose(
                        clearSearchInputTransformer
                    )
                )
            )
            .mergeWith(
                // Error for not implemented actions
                shared.filter { v ->
                  v != RefreshAction &&
                      v !is LoadNextPageAction &&
                      v != ClearFiltersAction &&
                      v !is ToggleFilterCompetitionAction &&
                      v !is ToggleFilterBroadcasterAction &&
                      v !is ToggleFilterTeamAction &&
                      v != LoadTagsAction &&
                      v !is SearchTeamAction &&
                      v != ClearSearchAction &&
                      v !is SearchedTeamSelectedAction &&
                      v != ClearSearchInputAction &&
                      v !is RefreshNotificationStatusAction
                }.flatMap { w ->
                  Observable.error<MatchesResult>(
                      IllegalArgumentException("Unknown Action type: " + w)
                  )
                })
      }
    }
}