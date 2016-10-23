package io.oldering.tvfoot.red.view;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.genius.groupie.GroupAdapter;
import com.genius.groupie.Item;

import javax.inject.Inject;

import io.oldering.tvfoot.red.R;
import io.oldering.tvfoot.red.databinding.ActivityMatchListBinding;
import io.oldering.tvfoot.red.di.DaggerAppComponent;
import io.oldering.tvfoot.red.util.schedulers.BaseSchedulerProvider;
import io.oldering.tvfoot.red.view.item.DayHeaderItem;
import io.oldering.tvfoot.red.viewmodel.MatchListViewModel;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

public class MatchListActivity extends AppCompatActivity implements View.OnClickListener {
    private GroupAdapter matchListGroupAdapter;
    @Inject
    MatchListViewModel matchListVM;
    @Inject
    BaseSchedulerProvider schedulerProvider;
    private PublishSubject<Integer> paginatorSubject;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean requestUnderWay;
    private int pageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerAppComponent.create().inject(this);

        paginatorSubject = PublishSubject.create();
        pageIndex = 0;

        ActivityMatchListBinding dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_match_list);
        Toolbar toolbar = dataBinding.matchListFilterToolbar;
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = dataBinding.matchListRecyclerView;
        matchListGroupAdapter = new GroupAdapter(this);
        recyclerView.setAdapter(matchListGroupAdapter);
        recyclerView.addOnScrollListener(new InfiniteScrollListener((LinearLayoutManager) recyclerView.getLayoutManager()) {
            @Override
            public void onLoadMore(int current_page) {
                Timber.d("onLoadMore %d, %d", current_page, pageIndex);
                if (requestUnderWay) return;

                paginatorSubject.onNext(++pageIndex);
            }
        });

        FloatingActionButton fab = dataBinding.matchListFilterFab;
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        bind();

//        progressBar.setVisibility(View.VISIBLE);
        paginatorSubject.onNext(pageIndex);
    }

    private void bind() {
        Disposable matchDisposable = paginatorSubject
                .doOnNext(integer -> {
                    requestUnderWay = true;
                    Timber.d("Loading page : %d", pageIndex);
                    // TODO _progressBar.setVisibility(View.VISIBLE);
                })
                .concatMap(matchListVM::getMatches)
                .observeOn(schedulerProvider.ui())
                .map(item -> {
                    MatchListActivity.this.addItem(item);
                    return 69; // dummy
                })
                .doOnNext(i -> {
                    requestUnderWay = false;
                    // TODO(benoit) _progressBar.setVisibility(View.INVISIBLE);
                })
                .subscribe();

        disposables.add(matchDisposable);
    }

    @Override
    protected void onStop() {
        super.onResume();
        unbind();
    }

    private void unbind() {
        disposables.clear();
    }

    private void addItem(Item item) {
        if (item instanceof DayHeaderItem) {
            Timber.d("addItem: is day header %s", ((DayHeaderItem) item).dayHeaderVM.getDisplayedDate());
        }
        matchListGroupAdapter.add(item);
    }

    @Override
    public void onClick(View v) {
        Timber.d("onClick: ");
    }
}
