package com.example.revolt.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.revolt.R;
import com.example.revolt.data.ApiCallback;
import com.example.revolt.data.ApiClient;
import com.example.revolt.data.model.PickupRequest;
import com.example.revolt.databinding.FragmentRequestsBinding;
import com.example.revolt.ui.request.NewRequestActivity;
import com.example.revolt.ui.request.RequestAdapter;
import com.example.revolt.ui.request.RequestDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class RequestsFragment extends Fragment {
    private FragmentRequestsBinding b;
    private RequestAdapter adapter;
    private List<PickupRequest> all = new ArrayList<>();
    private boolean loaded;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentRequestsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new RequestAdapter(r -> startActivity(RequestDetailActivity.intent(requireContext(), r.id)));
        b.list.setAdapter(adapter);
        b.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 8) b.fabNew.shrink();
                else if (dy < -8) b.fabNew.extend();
            }
        });

        b.filters.setOnCheckedStateChangeListener((group, ids) -> render());
        b.swipeRefresh.setColorSchemeResources(R.color.primary);
        b.swipeRefresh.setOnRefreshListener(this::load);
        b.swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                (b.list.getVisibility() == View.VISIBLE ? b.list : b.emptyState).canScrollVertically(-1));
        b.emptyRetry.setOnClickListener(v -> {
            b.swipeRefresh.setRefreshing(true);
            load();
        });
        b.fabNew.setOnClickListener(v ->
                startActivity(NewRequestActivity.intent(requireContext(), PickupRequest.TYPE_RECYCLE, 0)));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!loaded) b.swipeRefresh.setRefreshing(true);
        load();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    private void load() {
        ApiClient.get(requireContext()).myRequests().enqueue(new ApiCallback<List<PickupRequest>>(requireContext()) {
            @Override
            public void onSuccess(@NonNull List<PickupRequest> requests) {
                if (b == null) return;
                loaded = true;
                all = requests;
                b.swipeRefresh.setRefreshing(false);
                render();
            }

            @Override
            public void onError(@NonNull String message) {
                if (b == null) return;
                b.swipeRefresh.setRefreshing(false);
                if (all.isEmpty()) {
                    showEmpty(getString(R.string.error_generic), message, true);
                }
            }
        });
    }

    private void render() {
        if (b == null) return;
        int checked = b.filters.getCheckedChipId();
        List<PickupRequest> visible = new ArrayList<>();
        for (PickupRequest r : all) {
            if (checked == R.id.filterActive && !r.isActive()) continue;
            if (checked == R.id.filterDone && r.isActive()) continue;
            visible.add(r);
        }
        adapter.setItems(visible);

        if (!loaded) return;
        if (all.isEmpty()) {
            showEmpty(getString(R.string.requests_empty_title), getString(R.string.requests_empty_body), false);
        } else if (visible.isEmpty()) {
            showEmpty(getString(R.string.requests_empty_filtered), "", false);
        } else {
            b.emptyState.setVisibility(View.GONE);
            b.list.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty(String title, String body, boolean retry) {
        b.list.setVisibility(View.GONE);
        b.emptyState.setVisibility(View.VISIBLE);
        b.emptyTitle.setText(title);
        b.emptyBody.setText(body);
        b.emptyBody.setVisibility(body.isEmpty() ? View.GONE : View.VISIBLE);
        b.emptyRetry.setVisibility(retry ? View.VISIBLE : View.GONE);
    }
}
