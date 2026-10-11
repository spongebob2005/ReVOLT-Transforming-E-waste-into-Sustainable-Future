package com.example.revolt.ui.request;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.revolt.R;
import com.example.revolt.data.ApiCallback;
import com.example.revolt.data.ApiClient;
import com.example.revolt.data.model.Organization;
import com.example.revolt.data.model.PickupRequest;
import com.example.revolt.databinding.ActivityNewRequestBinding;
import com.example.revolt.util.Formats;
import com.example.revolt.util.ImageUtils;
import com.example.revolt.util.SystemBars;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/** One form for both recycle and reuse requests; replaces the old separate forms. */
public class NewRequestActivity extends AppCompatActivity {
    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_ORG_ID = "org_id";
    private static final String STATE_PHOTO = "photo";
    private static final String STATE_CAMERA = "camera";
    private static final String STATE_DATE = "date";
    private static final String DATE_PICKER_TAG = "date_picker";
    private static final MediaType TEXT = MediaType.get("text/plain; charset=utf-8");

    private ActivityNewRequestBinding b;
    private final List<Organization> orgs = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<String> categoryKeys;
    private List<String> categoryLabels;
    private long preselectedOrgId;
    @Nullable private Uri photoUri;
    @Nullable private Uri pendingCameraUri;
    @Nullable private String preferredDate;

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), saved -> {
                if (Boolean.TRUE.equals(saved) && pendingCameraUri != null) setPhoto(pendingCameraUri);
                pendingCameraUri = null;
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) setPhoto(uri);
            });

    public static Intent intent(Context context, String type, long organizationId) {
        return new Intent(context, NewRequestActivity.class)
                .putExtra(EXTRA_TYPE, type)
                .putExtra(EXTRA_ORG_ID, organizationId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityNewRequestBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        SystemBars.plain(this);
        b.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        categoryKeys = Arrays.asList(getResources().getStringArray(R.array.category_keys));
        categoryLabels = Arrays.asList(getResources().getStringArray(R.array.category_labels));
        b.inputCategory.setSimpleItems(categoryLabels.toArray(new String[0]));
        preselectedOrgId = getIntent().getLongExtra(EXTRA_ORG_ID, 0);

        b.typeToggle.addOnButtonCheckedListener((group, id, checked) -> {
            if (checked) b.typeHint.setText(id == R.id.btnTypeReuse ? R.string.reuse_desc : R.string.recycle_desc);
        });
        if (savedInstanceState == null) {
            boolean reuse = PickupRequest.TYPE_REUSE.equals(getIntent().getStringExtra(EXTRA_TYPE));
            b.typeToggle.check(reuse ? R.id.btnTypeReuse : R.id.btnTypeRecycle);
        } else {
            int checked = b.typeToggle.getCheckedButtonId();
            b.typeHint.setText(checked == R.id.btnTypeReuse ? R.string.reuse_desc : R.string.recycle_desc);
            photoUri = savedInstanceState.getParcelable(STATE_PHOTO);
            pendingCameraUri = savedInstanceState.getParcelable(STATE_CAMERA);
            preferredDate = savedInstanceState.getString(STATE_DATE);
            if (photoUri != null) setPhoto(photoUri);
        }
        updateDateField();

        b.btnCamera.setOnClickListener(v -> openCamera());
        b.btnGallery.setOnClickListener(v -> pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        b.photoPlaceholder.setOnClickListener(v -> openCamera());
        b.btnRemovePhoto.setOnClickListener(v -> clearPhoto());

        b.conditionGroup.setOnCheckedStateChangeListener((group, ids) -> b.conditionError.setVisibility(View.GONE));
        b.inputDate.setOnClickListener(v -> openDatePicker());
        b.layoutDate.setEndIconOnClickListener(v -> {
            preferredDate = null;
            updateDateField();
        });
        @SuppressWarnings("unchecked")
        MaterialDatePicker<Long> restoredPicker =
                (MaterialDatePicker<Long>) getSupportFragmentManager().findFragmentByTag(DATE_PICKER_TAG);
        if (restoredPicker != null) restoredPicker.addOnPositiveButtonClickListener(this::onDatePicked);

        b.btnSubmit.setOnClickListener(v -> submit());
        loadOrganizations();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_PHOTO, photoUri);
        outState.putParcelable(STATE_CAMERA, pendingCameraUri);
        outState.putString(STATE_DATE, preferredDate);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    // region Photo

    private void openCamera() {
        try {
            pendingCameraUri = ImageUtils.newCameraUri(this);
            takePicture.launch(pendingCameraUri);
        } catch (IOException | ActivityNotFoundException e) {
            pendingCameraUri = null;
            snackbar(getString(R.string.no_camera_app));
        }
    }

    private void setPhoto(Uri uri) {
        photoUri = uri;
        b.photo.setVisibility(View.VISIBLE);
        b.photoPlaceholder.setVisibility(View.GONE);
        b.btnRemovePhoto.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).into(b.photo);
    }

    private void clearPhoto() {
        photoUri = null;
        Glide.with(this).clear(b.photo);
        b.photo.setVisibility(View.GONE);
        b.photoPlaceholder.setVisibility(View.VISIBLE);
        b.btnRemovePhoto.setVisibility(View.GONE);
    }

    // endregion

    // region Date

    private void openDatePicker() {
        long today = MaterialDatePicker.todayInUtcMilliseconds();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.hint_date)
                .setSelection(today)
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setStart(today)
                        .setValidator(DateValidatorPointForward.now())
                        .build())
                .build();
        picker.addOnPositiveButtonClickListener(this::onDatePicked);
        picker.show(getSupportFragmentManager(), DATE_PICKER_TAG);
    }

    private void onDatePicked(Long utcMillis) {
        // The picker returns midnight UTC of the chosen day, so format in UTC to keep the same day.
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));
        preferredDate = iso.format(new Date(utcMillis));
        updateDateField();
    }

    private void updateDateField() {
        b.inputDate.setText(preferredDate == null ? "" : Formats.date(preferredDate));
        b.layoutDate.setEndIconVisible(preferredDate != null);
    }

    // endregion

    private void loadOrganizations() {
        ApiClient.get(this).organizations().enqueue(new ApiCallback<List<Organization>>(this) {
            @Override
            public void onSuccess(@NonNull List<Organization> body) {
                if (isDestroyed()) return;
                orgs.clear();
                orgs.addAll(body);
                String[] names = new String[orgs.size()];
                for (int i = 0; i < orgs.size(); i++) names[i] = orgs.get(i).name;
                b.inputOrganization.setSimpleItems(names);
                if (textOf(b.layoutOrganization).isEmpty()) {
                    for (Organization o : orgs) {
                        if (o.id == preselectedOrgId) b.inputOrganization.setText(o.name, false);
                    }
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (isDestroyed()) return;
                b.layoutOrganization.setError(message);
            }
        });
    }

    @Nullable
    private Organization selectedOrganization() {
        String name = textOf(b.layoutOrganization);
        for (Organization o : orgs) if (o.name.equals(name)) return o;
        return null;
    }

    private void submit() {
        String deviceName = textOf(b.layoutDeviceName);
        int categoryIndex = categoryLabels.indexOf(textOf(b.layoutCategory));
        int quantity = parseInt(textOf(b.layoutQuantity));
        String condition = selectedCondition();
        Organization org = selectedOrganization();
        String address = textOf(b.layoutAddress);

        boolean ok = check(b.layoutDeviceName, deviceName.length() >= 2, getString(R.string.error_required));
        ok &= check(b.layoutCategory, categoryIndex >= 0, getString(R.string.error_required));
        ok &= check(b.layoutQuantity, quantity >= 1 && quantity <= 50, getString(R.string.error_quantity));
        b.conditionError.setVisibility(condition == null ? View.VISIBLE : View.GONE);
        ok &= condition != null;
        ok &= check(b.layoutOrganization, org != null,
                getString(orgs.isEmpty() ? R.string.error_orgs_loading : R.string.error_required));
        ok &= check(b.layoutAddress, address.length() >= 5, getString(R.string.error_required));
        if (!ok) {
            snackbar(getString(R.string.error_required));
            return;
        }

        Map<String, RequestBody> fields = new HashMap<>();
        fields.put("type", text(b.typeToggle.getCheckedButtonId() == R.id.btnTypeReuse
                ? PickupRequest.TYPE_REUSE : PickupRequest.TYPE_RECYCLE));
        fields.put("deviceName", text(deviceName));
        fields.put("category", text(categoryKeys.get(categoryIndex)));
        fields.put("quantity", text(String.valueOf(quantity)));
        fields.put("condition", text(condition));
        fields.put("description", text(textOf(b.layoutDescription)));
        fields.put("organizationId", text(String.valueOf(org.id)));
        fields.put("pickupAddress", text(address));
        if (preferredDate != null) fields.put("preferredDate", text(preferredDate));

        setLoading(true);
        Uri photo = photoUri;
        executor.execute(() -> {
            File compressed = null;
            if (photo != null) {
                try {
                    compressed = ImageUtils.compressForUpload(this, photo);
                } catch (IOException | RuntimeException e) {
                    runOnUiThread(() -> {
                        if (isDestroyed()) return;
                        setLoading(false);
                        snackbar(getString(R.string.photo_failed));
                    });
                    return;
                }
            }
            File upload = compressed;
            runOnUiThread(() -> send(fields, upload));
        });
    }

    private void send(Map<String, RequestBody> fields, @Nullable File photo) {
        if (isDestroyed()) return;
        MultipartBody.Part image = photo == null ? null : MultipartBody.Part.createFormData(
                "image", photo.getName(), RequestBody.create(photo, MediaType.get("image/jpeg")));

        ApiClient.get(this).createRequest(fields, image).enqueue(new ApiCallback<PickupRequest>(this) {
            @Override
            public void onSuccess(@NonNull PickupRequest request) {
                deleteQuietly(photo);
                if (isDestroyed()) return;
                startActivity(SuccessActivity.intent(NewRequestActivity.this, request.id, request.organization.name));
                finish();
            }

            @Override
            public void onError(@NonNull String message) {
                deleteQuietly(photo);
                if (isDestroyed()) return;
                setLoading(false);
                snackbar(message);
            }
        });
    }

    @Nullable
    private String selectedCondition() {
        int id = b.conditionGroup.getCheckedChipId();
        if (id == R.id.chipWorking) return "working";
        if (id == R.id.chipPartial) return "partial";
        if (id == R.id.chipNotWorking) return "not_working";
        return null;
    }

    private void setLoading(boolean loading) {
        b.btnSubmit.setEnabled(!loading);
        b.btnSubmit.setText(loading ? R.string.submitting : R.string.action_submit_request);
        b.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void snackbar(String message) {
        Snackbar.make(b.getRoot(), message, Snackbar.LENGTH_LONG).setAnchorView(b.btnSubmit).show();
    }

    private static boolean check(TextInputLayout layout, boolean valid, String error) {
        layout.setError(valid ? null : error);
        return valid;
    }

    private static String textOf(TextInputLayout layout) {
        return layout.getEditText() == null || layout.getEditText().getText() == null
                ? "" : layout.getEditText().getText().toString().trim();
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static RequestBody text(String value) {
        return RequestBody.create(value, TEXT);
    }

    private static void deleteQuietly(@Nullable File file) {
        if (file != null && !file.delete()) file.deleteOnExit();
    }
}
