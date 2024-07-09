package com.misterchan.charmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    public interface OnItemClickListener {
        void onClick(int codepoint);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tv;

        public ViewHolder(View view) {
            super(view);
            tv = view.findViewById(R.id.tv);
        }
    }

    private final OnItemClickListener listener;

    public Adapter(OnItemClickListener l) {
        listener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tv.setText(String.valueOf(Character.toChars(position)));
        holder.itemView.setOnClickListener(v -> listener.onClick(position));
    }

    @Override
    public int getItemCount() {
        return 0x110000;
    }
}
