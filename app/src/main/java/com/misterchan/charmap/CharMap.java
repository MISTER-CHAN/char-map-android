package com.misterchan.charmap;

import android.content.res.TypedArray;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.misterchan.charmap.databinding.InputBinding;

public class CharMap extends InputMethodService {
    private InputBinding input;
    private InputConnection ic;

    private final AdapterView.OnItemClickListener onDropdownItemClickListener = new AdapterView.OnItemClickListener() {
        private static final int[] codepoints = {0x0, 0x1000, 0x2000, 0x3000, 0x4000, 0xA000, 0xB000, 0xF000, 0x10000};

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            onDropdownMenuItemClick(codepoints[position]);
        }
    };

    private final RecyclerView.OnScrollListener onMapScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            onMapScroll();
        }
    };

    @Override
    public View onCreateInputView() {
        input = InputBinding.inflate(getLayoutInflater());
        ic = getCurrentInputConnection();

        input.actv.setAdapter(new ArrayAdapter(this, R.layout.dropdown_item, getResources().getStringArray(R.array.items)));
        input.actv.setOnItemClickListener(onDropdownItemClickListener);
        input.bGo.setOnClickListener(v -> onGoButtonClick());
        input.bBackspace.setOnClickListener(v -> onBackspaceButtonClick());
        input.bPageUp.setOnClickListener(v -> onPageUpButtonClick());
        input.bPageDown.setOnClickListener(v -> onPageDownButtonClick());
        input.rv.setLayoutManager(new GridLayoutManager(input.rv.getContext(), 0x10));
        input.rv.setAdapter(new Adapter(this::onMapItemClick));
        input.rv.addOnScrollListener(onMapScrollListener);

        return input.getRoot();
    }

    private void onDropdownMenuItemClick(int codepoint) {
        input.actv.setText(Integer.toHexString(codepoint).toUpperCase());
    }

    private void onGoButtonClick() {
        CharSequence text = input.actv.getText();
        int codepoint;
        if (text.length() == 0) {
            return;
        } else if (text.length() == 1) {
            codepoint = Character.codePointAt(text, 0);
            input.actv.setText(String.valueOf(codepoint));
        } else {
            try {
                codepoint = Integer.parseUnsignedInt(text.toString(), 0x10);
            } catch (NumberFormatException e) {
                return;
            }
        }
        input.rv.scrollToPosition(codepoint);
        input.actv.clearFocus();
    }

    private void onBackspaceButtonClick() {
        if (input.actv.hasFocus()) {
            int selSt = input.actv.getSelectionStart(), selEn = input.actv.getSelectionEnd();
            if (selSt == selEn) {
                input.actv.getText().delete(selSt - 1, selEn);
            } else {
                input.actv.getText().delete(selSt, selEn);
            }
        } else {
            ic.deleteSurroundingText(1, 0);
        }
    }

    private void onPageUpButtonClick() {

    }

    private void onPageDownButtonClick() {

    }

    private void onMapItemClick(int codepoint) {
        String text = String.valueOf(Character.toChars(codepoint));
        if (input.actv.hasFocus()) {
            int selSt = input.actv.getSelectionStart(), selEn = input.actv.getSelectionEnd();
            if (selSt != selEn) {
                input.actv.getText().delete(selSt, selEn);
            }
            input.actv.getText().insert(selSt, text);
        } else {
            ic.commitText(String.valueOf(Character.toChars(codepoint)), 1);
            input.actv.setText(Integer.toHexString(codepoint));
        }
    }

    private void onMapScroll() {

    }
}
