package com.example.revolt.ui.org;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.revolt.data.model.Organization;
import com.example.revolt.databinding.ItemOrganizationBinding;
import com.example.revolt.util.OrgLogos;

import java.util.ArrayList;
import java.util.List;

public class OrganizationAdapter extends RecyclerView.Adapter<OrganizationAdapter.Holder> {
    public interface OnClick {
        void onOrganization(Organization org);
    }

    private final List<Organization> items = new ArrayList<>();
    private final OnClick onClick;

    public OrganizationAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<Organization> orgs) {
        items.clear();
        items.addAll(orgs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemOrganizationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Organization org = items.get(position);
        holder.b.logo.setImageResource(OrgLogos.forKey(org.logoKey));
        holder.b.name.setText(org.name);
        holder.b.city.setText(org.city);
        holder.b.getRoot().setOnClickListener(v -> onClick.onOrganization(org));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemOrganizationBinding b;

        Holder(ItemOrganizationBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
