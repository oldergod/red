package com.benoitquenaudon.tvfoot.red.app.domain.match

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.benoitquenaudon.rxdatabinding.databinding.RxObservableBoolean
import com.benoitquenaudon.tvfoot.red.AUTHORITIES
import com.benoitquenaudon.tvfoot.red.PATH_MATCH
import com.benoitquenaudon.tvfoot.red.R
import com.benoitquenaudon.tvfoot.red.SCHEMES
import com.benoitquenaudon.tvfoot.red.app.common.BaseActivity
import com.benoitquenaudon.tvfoot.red.app.common.flowcontroller.FlowController
import com.benoitquenaudon.tvfoot.red.app.common.notification.MINUTES_BEFORE_NOTIFICATION
import com.benoitquenaudon.tvfoot.red.app.data.entity.Match
import com.benoitquenaudon.tvfoot.red.app.data.entity.Match.Companion.MATCH_ID
import com.benoitquenaudon.tvfoot.red.app.mvi.MviView
import com.benoitquenaudon.tvfoot.red.databinding.ActivityMatchBinding
import com.benoitquenaudon.tvfoot.red.util.errorHandlingSubscribe
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class MatchActivity : BaseActivity(), MviView<MatchIntent, MatchViewState> {
  @Inject
  lateinit var matchBroadcastersAdapter: MatchBroadcastersAdapter
  @Inject
  lateinit var flowController: FlowController
  @Inject
  lateinit var disposables: CompositeDisposable
  @Inject
  lateinit var bindingModel: MatchBindingModel
  @Inject
  lateinit var viewModelFactory: ViewModelProvider.Factory
  private val viewModel: MatchViewModel by lazy(NONE) {
    ViewModelProviders.of(this, viewModelFactory).get(MatchViewModel::class.java)
  }

  private val binding: ActivityMatchBinding by lazy(NONE) {
    DataBindingUtil.setContentView<ActivityMatchBinding>(this, R.layout.activity_match)
  }
  private var matchId: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val intent = intent
    matchId = intent.getStringExtra(Match.MATCH_ID)

    val uri: Uri? = intent.data
    if (matchId == null && AUTHORITIES.contains(uri?.authority) && SCHEMES.contains(uri?.scheme)) {
      val segments = uri?.pathSegments
      if (segments?.size == 5 && PATH_MATCH == segments[0]) {
        matchId = segments[4]
      }
    }

    if (matchId == null) {
      Timber.w("matchDisplayable id is null %s", uri)
      Toast.makeText(this, "Cannot find any record for this match.", Toast.LENGTH_LONG)
          .show()
      flowController.toMatches()
      finish()
      return
    }

    setupView()

    Timber.d("matchDisplayable with load with id %s", matchId)
    bind()
  }

  private fun setupView() {
    binding.matchDetailBroadcasters.adapter = matchBroadcastersAdapter
    binding.bindingModel = bindingModel

    setSupportActionBar(binding.matchToolbar)
    supportActionBar?.run {
      setDisplayShowTitleEnabled(false)
      setDisplayHomeAsUpEnabled(true)
    }
  }

  private fun bind() {
    disposables.add(viewModel.states().errorHandlingSubscribe(this::render))
    viewModel.processIntents(intents())

    disposables.add(RxObservableBoolean.propertyChanges(bindingModel.shouldNotifyMatchStart)
        .errorHandlingSubscribe { shouldNotifyMatchStart ->
          if (shouldNotifyMatchStart) {
            Snackbar.make(binding.root,
                resources.getQuantityString(
                    R.plurals.will_be_notify_desc,
                    MINUTES_BEFORE_NOTIFICATION,
                    MINUTES_BEFORE_NOTIFICATION),
                Snackbar.LENGTH_LONG).show()
          }
        })
  }

  override fun onDestroy() {
    super.onDestroy()
    disposables.dispose()
  }

  override fun intents(): Observable<MatchIntent> {
    return Observable.merge(initialIntent(), fabClickIntent())
  }

  private fun initialIntent(): Observable<MatchIntent.InitialIntent> {
    return Observable.just(MatchIntent.InitialIntent(checkNotNull(matchId, { "MatchId is null" })))
  }

  private fun fabClickIntent(): Observable<MatchIntent.NotifyMatchStartIntent> {
    return RxView.clicks(binding.notifyMatchStartFab)
        .map {
          val match = bindingModel.match.get()!!
          MatchIntent.NotifyMatchStartIntent(
              match.matchId,
              match.startAt,
              !isMatchNotificationActivated
          )
        }
  }

  private val isMatchNotificationActivated: Boolean
    get() = binding.notifyMatchStartFab.isActivated

  override fun render(state: MatchViewState) {
    bindingModel.updateFromState(state)

    if (state.match != null) {
      val intent = Intent("REFRESH_NOTIFICATION_STATUS")
      intent.putExtra(MATCH_ID, state.match.matchId)
      setResult(RESULT_OK, intent)
    }
  }
}
