package com.example.revolt.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.revolt.R;
import com.example.revolt.data.ApiCallback;
import com.example.revolt.data.ApiClient;
import com.example.revolt.data.SessionManager;
import com.example.revolt.data.model.Organization;
import com.example.revolt.data.model.PickupRequest;
import com.example.revolt.data.model.Stats;
import com.example.revolt.data.model.User;
import com.example.revolt.databinding.FragmentHomeBinding;
import com.example.revolt.databinding.ViewActionCardBinding;
import com.example.revolt.ui.org.OrganizationActivity;
import com.example.revolt.ui.org.OrganizationAdapter;
import com.example.revolt.ui.request.NewRequestActivity;
import com.example.revolt.util.Formats;

import java.util.List;
import java.util.Random;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding b;
    private OrganizationAdapter partners;
    private int pendingLoads;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentHomeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bindAction(b.cardRecycle, R.drawable.recycle, R.string.recycle, R.string.recycle_desc, PickupRequest.TYPE_RECYCLE);
        bindAction(b.cardReuse, R.drawable.reuse, R.string.reuse, R.string.reuse_desc, PickupRequest.TYPE_REUSE);

        b.statRequests.label.setText(R.string.stat_requests);
        b.statActive.label.setText(R.string.stat_active);
        b.statKg.label.setText(R.string.stat_kg);

        partners = new OrganizationAdapter(org ->
                startActivity(OrganizationActivity.intent(requireContext(), org)));
        b.listPartners.setAdapter(partners);

        String[] facts = getResources().getStringArray(R.array.eco_facts);
        b.fact.setText(facts[new Random().nextInt(facts.length)]);

        b.avatar.setOnClickListener(v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_profile));
        b.swipeRefresh.setColorSchemeResources(R.color.primary);
        b.swipeRefresh.setOnRefreshListener(this::load);
    }

    @Override
    public void onResume() {
        super.onResume();
        User user = SessionManager.get(requireContext()).getUser();
        if (user != null) {
            b.greeting.setText(getString(R.string.home_greeting, Formats.firstName(user.name)));
            b.avatar.setText(Formats.initials(user.name));
        }
        load();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    private void bindAction(ViewActionCardBinding card, int icon, int title, int description, String type) {
        card.icon.setImageResource(icon);
        card.title.setText(title);
        card.description.setText(description);
        card.getRoot().setOnClickListener(v ->
                startActivity(NewRequestActivity.intent(requireContext(), type, 0)));
    }

    private void load() {
        pendingLoads = 2;

        ApiClient.get(requireContext()).myStats().enqueue(new ApiCallback<Stats>(requireContext()) {
            @Override
            public void onSuccess(@NonNull Stats stats) {
                if (b == null) return;
                b.statRequests.value.setText(String.valueOf(stats.totalRequests));
                b.statActive.value.setText(String.valueOf(stats.activeRequests));
                b.statKg.value.setText(Formats.kg(stats.kgDiverted));
                loadFinished();
            }

            @Override
            public void onError(@NonNull String message) {
                loadFinished();
            }
        });

        ApiClient.get(requireContext()).organizations().enqueue(new ApiCallback<List<Organization>>(requireContext()) {
            @Override
            public void onSuccess(@NonNull List<Organization> orgs) {
                if (b == null) return;
                partners.setItems(orgs);
                b.partnersError.setVisibility(View.GONE);
                loadFinished();
            }

            @Override
            public void onError(@NonNull String message) {
                if (b == null) return;
                if (partners.getItemCount() == 0) {
                    b.partnersError.setText(message);
                    b.partnersError.setVisibility(View.VISIBLE);
                }
                loadFinished();
            }
        });
    }

    private void loadFinished() {
        if (--pendingLoads <= 0 && b != null) b.swipeRefresh.setRefreshing(false);
    }
}
