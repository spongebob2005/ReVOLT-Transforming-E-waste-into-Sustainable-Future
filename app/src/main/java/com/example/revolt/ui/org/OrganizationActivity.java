package com.example.revolt.ui.org;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.revolt.data.model.Organization;
import com.example.revolt.data.model.PickupRequest;
import com.example.revolt.databinding.ActivityOrganizationBinding;
import com.example.revolt.ui.request.NewRequestActivity;
import com.example.revolt.util.Links;
import com.example.revolt.util.OrgLogos;
import com.example.revolt.util.SystemBars;

/** Partner profile; replaces the four hard-coded NGO screens. */
public class OrganizationActivity extends AppCompatActivity {
    private static final String EXTRA_ORG = "org";

    public static Intent intent(Context context, Organization org) {
        return new Intent(context, OrganizationActivity.class).putExtra(EXTRA_ORG, org);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityOrganizationBinding b = ActivityOrganizationBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        SystemBars.plain(this);
        b.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        @SuppressWarnings("deprecation")
        Organization org = (Organization) getIntent().getSerializableExtra(EXTRA_ORG);
        if (org == null) {
            finish();
            return;
        }

        b.logo.setImageResource(OrgLogos.forKey(org.logoKey));
        b.name.setText(org.name);
        b.tagline.setText(org.tagline);
        b.city.setText(org.city);
        b.about.setText(org.about);

        if (org.website == null || org.website.isEmpty()) {
            b.btnWebsite.setVisibility(View.GONE);
        } else {
            b.btnWebsite.setOnClickListener(v -> Links.open(this, Uri.parse(org.website)));
        }
        b.btnDirections.setOnClickListener(v -> Links.directionsTo(this, org.name + ", " + org.city));
        b.btnRecycle.setOnClickListener(v ->
                startActivity(NewRequestActivity.intent(this, PickupRequest.TYPE_RECYCLE, org.id)));
        b.btnReuse.setOnClickListener(v ->
                startActivity(NewRequestActivity.intent(this, PickupRequest.TYPE_REUSE, org.id)));
    }
}
