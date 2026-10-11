package com.example.revolt.ui.request;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.revolt.R;
import com.example.revolt.data.ApiClient;
import com.example.revolt.data.model.PickupRequest;
import com.example.revolt.databinding.ItemRequestBinding;
import com.example.revolt.util.Formats;
import com.example.revolt.util.StatusStyle;

import java.util.ArrayList;
import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.Holder> {
    public interface OnClick {
        void onRequest(PickupRequest request);
    }

    private final List<PickupRequest> items = new ArrayList<>();
    private final OnClick onClick;

    public RequestAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<PickupRequest> requests) {
        items.clear();
        items.addAll(requests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        PickupRequest r = items.get(position);
        Context c = holder.itemView.getContext();
        String typeLabel = c.getString(PickupRequest.TYPE_REUSE.equals(r.type) ? R.string.reuse : R.string.recycle);

        holder.b.title.setText(r.quantity > 1 ? r.deviceName + " ×" + r.quantity : r.deviceName);
        holder.b.meta.setText(c.getString(R.string.request_item_meta, typeLabel, r.organization.name));
        holder.b.date.setText(Formats.date(r.createdAt));
        StatusStyle.apply(holder.b.status, r.status, r.statusLabel);
        loadThumb(holder.b.thumb, r);
        holder.b.getRoot().setOnClickListener(v -> onClick.onRequest(r));
    }

    static void loadThumb(ImageView view, PickupRequest r) {
        int placeholder = PickupRequest.TYPE_REUSE.equals(r.type) ? R.drawable.reuse : R.drawable.recycle;
        String url = ApiClient.absoluteUrl(view.getContext(), r.imageUrl);
        if (url == null) {
            Glide.with(view).clear(view);
            view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            view.setImageResource(placeholder);
            int pad = view.getResources().getDimensionPixelSize(R.dimen.field_radius);
            view.setPadding(pad, pad, pad, pad);
        } else {
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            view.setPadding(0, 0, 0, 0);
            Glide.with(view).load(url).placeholder(placeholder).error(placeholder).into(view);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemRequestBinding b;

        Holder(ItemRequestBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
