package com.example.revolt.ui.request;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.revolt.R;
import com.example.revolt.data.ApiCallback;
import com.example.revolt.data.ApiClient;
import com.example.revolt.data.model.PickupRequest;
import com.example.revolt.data.model.RequestEvent;
import com.example.revolt.databinding.ActivityRequestDetailBinding;
import com.example.revolt.databinding.ItemTimelineBinding;
import com.example.revolt.databinding.ViewInfoRowBinding;
import com.example.revolt.util.Formats;
import com.example.revolt.util.Links;
import com.example.revolt.util.StatusStyle;
import com.example.revolt.util.SystemBars;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class RequestDetailActivity extends AppCompatActivity {
    private static final String EXTRA_ID = "id";

    private ActivityRequestDetailBinding b;
    private long requestId;

    public static Intent intent(Context context, long requestId) {
        return new Intent(context, RequestDetailActivity.class).putExtra(EXTRA_ID, requestId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRequestDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        SystemBars.plain(this);

        requestId = getIntent().getLongExtra(EXTRA_ID, 0);
        b.toolbar.setTitle(getString(R.string.request_detail_title, (int) requestId));
        b.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        b.swipeRefresh.setColorSchemeResources(R.color.primary);
        b.swipeRefresh.setOnRefreshListener(this::load);
        // The scroll view is wrapped in a FrameLayout, so tell the refresh layout when it can scroll.
        b.swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                b.content.getVisibility() == View.VISIBLE && b.content.canScrollVertically(-1));
        b.btnRetry.setOnClickListener(v -> {
            b.swipeRefresh.setRefreshing(true);
            load();
        });

        b.swipeRefresh.setRefreshing(true);
        load();
    }

    private void load() {
        ApiClient.get(this).request(requestId).enqueue(new ApiCallback<PickupRequest>(this) {
            @Override
            public void onSuccess(@NonNull PickupRequest request) {
                if (isDestroyed()) return;
                b.swipeRefresh.setRefreshing(false);
                bind(request);
            }

            @Override
            public void onError(@NonNull String message) {
                if (isDestroyed()) return;
                b.swipeRefresh.setRefreshing(false);
                if (b.content.getVisibility() == View.VISIBLE) {
                    Snackbar.make(b.getRoot(), message, Snackbar.LENGTH_LONG).show();
                } else {
                    b.errorText.setText(message);
                    b.errorState.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void bind(PickupRequest r) {
        b.errorState.setVisibility(View.GONE);
        b.content.setVisibility(View.VISIBLE);

        String imageUrl = ApiClient.absoluteUrl(this, r.imageUrl);
        if (imageUrl == null) {
            b.photo.setVisibility(View.GONE);
        } else {
            b.photo.setVisibility(View.VISIBLE);
            Glide.with(this).load(imageUrl).into(b.photo);
        }

        b.title.setText(r.deviceName);
        StatusStyle.apply(b.status, r.status, r.statusLabel);
        boolean reuse = PickupRequest.TYPE_REUSE.equals(r.type);
        b.type.setText(reuse ? R.string.reuse : R.string.recycle);
        b.type.setChipIconResource(R.drawable.ic_eco);
        b.type.setChipIconVisible(true);
        b.submitted.setText(Formats.dateTime(r.createdAt));

        row(b.rowCategory, R.drawable.ic_category, R.string.label_category, r.categoryLabel + " · " + r.conditionLabel);
        row(b.rowQuantity, R.drawable.ic_numbers, R.string.label_quantity, String.valueOf(r.quantity));
        row(b.rowPartner, R.drawable.ic_business, R.string.label_partner,
                r.organization.name + (r.organization.city == null ? "" : " · " + r.organization.city));
        row(b.rowAddress, R.drawable.ic_location, R.string.label_address, r.pickupAddress);
        row(b.rowDate, R.drawable.ic_event, R.string.label_preferred_date,
                r.preferredDate == null ? getString(R.string.not_specified) : Formats.date(r.preferredDate));
        row(b.rowWeight, R.drawable.ic_eco, R.string.label_weight, getString(R.string.weight_kg, Formats.kg(r.estimatedWeightKg)));
        boolean hasNotes = r.description != null && !r.description.isEmpty();
        b.rowNotes.getRoot().setVisibility(hasNotes ? View.VISIBLE : View.GONE);
        if (hasNotes) row(b.rowNotes, R.drawable.ic_notes, R.string.label_notes, r.description);

        bindTimeline(r.events);

        String destination = r.organization.name + (r.organization.city == null ? "" : ", " + r.organization.city);
        b.btnDirections.setOnClickListener(v -> Links.directionsTo(this, destination));
        b.btnCancel.setVisibility(r.isCancellable() ? View.VISIBLE : View.GONE);
        b.btnCancel.setOnClickListener(v -> confirmCancel());
    }

    private void row(ViewInfoRowBinding row, @DrawableRes int icon, @StringRes int label, String value) {
        row.icon.setImageResource(icon);
        row.label.setText(label);
        row.value.setText(value);
    }

    private void bindTimeline(List<RequestEvent> events) {
        b.timeline.removeAllViews();
        if (events == null) return;
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < events.size(); i++) {
            RequestEvent e = events.get(i);
            ItemTimelineBinding item = ItemTimelineBinding.inflate(inflater, b.timeline, false);
            StatusStyle style = StatusStyle.of(e.status);
            item.dot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, style.background)));
            item.dotIcon.setImageResource(style.icon);
            item.dotIcon.setColorFilter(ContextCompat.getColor(this, style.foreground));
            item.status.setText(e.statusLabel);
            item.note.setText(e.note);
            item.note.setVisibility(e.note == null || e.note.isEmpty() ? View.GONE : View.VISIBLE);
            item.time.setText(Formats.dateTime(e.createdAt));
            item.lineTop.setVisibility(i == 0 ? View.INVISIBLE : View.VISIBLE);
            item.lineBottom.setVisibility(i == events.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            b.timeline.addView(item.getRoot());
        }
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cancel_confirm_title)
                .setMessage(R.string.cancel_confirm_body)
                .setNegativeButton(R.string.keep, null)
                .setPositiveButton(R.string.cancel_confirm_yes, (d, w) -> cancelRequest())
                .show();
    }

    private void cancelRequest() {
        b.btnCancel.setEnabled(false);
        ApiClient.get(this).cancelRequest(requestId).enqueue(new ApiCallback<PickupRequest>(this) {
            @Override
            public void onSuccess(@NonNull PickupRequest request) {
                if (isDestroyed()) return;
                b.btnCancel.setEnabled(true);
                bind(request);
                Snackbar.make(b.getRoot(), R.string.request_cancelled, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull String message) {
                if (isDestroyed()) return;
                b.btnCancel.setEnabled(true);
                Snackbar.make(b.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
