package com.misterchan.charmap;

import android.content.Context;
import android.widget.Filterable;

import androidx.annotation.NonNull;

public class ArrayAdapter extends android.widget.ArrayAdapter<String> implements Filterable {
    private static class Filter extends android.widget.Filter {
        private final Object[] array;

        private Filter(Object[] array) {
            this.array = array;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults fr = new FilterResults();
            fr.values = array;
            fr.count = array.length;
            return fr;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
        }
    }

    private final Filter filter;

    public ArrayAdapter(@NonNull Context context, int resource, @NonNull String[] objects) {
        super(context, resource, objects);
        filter = new Filter(objects);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }
}
