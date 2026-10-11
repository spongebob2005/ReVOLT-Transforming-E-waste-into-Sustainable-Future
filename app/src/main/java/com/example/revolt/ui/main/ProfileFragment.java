package com.example.revolt.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.revolt.R;
import com.example.revolt.data.ApiCallback;
import com.example.revolt.data.ApiClient;
import com.example.revolt.data.SessionManager;
import com.example.revolt.data.model.Stats;
import com.example.revolt.data.model.User;
import com.example.revolt.databinding.FragmentProfileBinding;
import com.example.revolt.databinding.ViewProfileRowBinding;
import com.example.revolt.ui.auth.LoginActivity;
import com.example.revolt.util.Formats;
import com.example.revolt.util.ServerDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding b;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.statCompleted.label.setText(R.string.stat_completed);
        b.statDevices.label.setText(R.string.stat_devices);
        b.statKg.label.setText(R.string.stat_kg);

        row(b.rowServer, R.drawable.ic_tune, R.string.server_settings);
        row(b.rowAbout, R.drawable.ic_info, R.string.about_app);
        row(b.rowLogout, R.drawable.ic_logout, R.string.action_logout);
        int error = ContextCompat.getColor(requireContext(), R.color.error);
        b.rowLogout.title.setTextColor(error);
        b.rowLogout.icon.setColorFilter(error);
        b.rowLogout.chevron.setVisibility(View.GONE);

        b.rowServer.getRoot().setOnClickListener(v -> ServerDialog.show(requireContext(), this::showServer));
        b.rowAbout.getRoot().setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.about_app)
                .setIcon(R.drawable.ic_eco)
                .setMessage(R.string.about_app_body)
                .setPositiveButton(R.string.ok, null)
                .show());
        b.rowLogout.getRoot().setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.action_logout, (d, w) -> logout())
                .show());

        try {
            String version = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            b.version.setText(getString(R.string.version, version));
        } catch (Exception e) {
            b.version.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        bindUser(SessionManager.get(requireContext()).getUser());
        showServer();
        load();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    private void row(ViewProfileRowBinding row, int icon, int title) {
        row.icon.setImageResource(icon);
        row.title.setText(title);
    }

    private void showServer() {
        if (b == null) return;
        b.rowServer.subtitle.setText(SessionManager.get(requireContext()).getServerUrl());
        b.rowServer.subtitle.setVisibility(View.VISIBLE);
    }

    private void bindUser(@Nullable User user) {
        if (user == null || b == null) return;
        b.avatar.setText(Formats.initials(user.name));
        b.name.setText(user.name);
        b.email.setText(user.email);
        b.phone.setText(user.phone);
        b.memberSince.setText(getString(R.string.member_since, Formats.monthYear(user.createdAt)));
    }

    private void load() {
        ApiClient.get(requireContext()).myStats().enqueue(new ApiCallback<Stats>(requireContext()) {
            @Override
            public void onSuccess(@NonNull Stats stats) {
                if (b == null) return;
                b.statCompleted.value.setText(String.valueOf(stats.completedRequests));
                b.statDevices.value.setText(String.valueOf(stats.devicesHandled));
                b.statKg.value.setText(Formats.kg(stats.kgDiverted));
            }

            @Override
            public void onError(@NonNull String message) {
            }
        });

        ApiClient.get(requireContext()).me().enqueue(new ApiCallback<User>(requireContext()) {
            @Override
            public void onSuccess(@NonNull User user) {
                if (b == null) return;
                SessionManager.get(requireContext()).updateUser(user);
                bindUser(user);
            }

            @Override
            public void onError(@NonNull String message) {
            }
        });
    }

    private void logout() {
        SessionManager.get(requireContext()).clear();
        startActivity(new Intent(requireContext(), LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
}
