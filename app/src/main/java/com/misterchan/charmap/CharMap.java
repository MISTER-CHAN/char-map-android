package com.misterchan.charmap;

import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.misterchan.charmap.databinding.InputBinding;

import java.util.ArrayList;
import java.util.List;

public class CharMap extends InputMethodService {
    private GridLayoutManager layoutManager;
    private InputBinding input;
    private int position;

    private final AdapterView.OnItemClickListener onDropdownItemClickListener = new AdapterView.OnItemClickListener() {
        private static final int[] codePoints = {0x0, 0x1000, 0x2000, 0x3000, 0x4000, 0xA000, 0xB000, 0xF000, 0x10000};

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            onDropdownItemClick(codePoints[position]);
        }
    };

    private final RecyclerView.OnScrollListener onMapScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            onMapScroll(dy);
        }
    };

    @Override
    public View onCreateInputView() {
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        layoutManager = new GridLayoutManager(this, isLandscape ? 0x20 : 0x10);
        input = InputBinding.inflate(getLayoutInflater());

        if (isLandscape) {
            ViewGroup.LayoutParams lp = input.rv.getLayoutParams();
            lp.height >>= 1;
        }

        input.bBack.setOnClickListener(v -> onBackButtonClick());
        input.actv.setAdapter(new ArrayAdapter(this, R.layout.dropdown_item, getResources().getStringArray(R.array.dropdown_items)));
        input.actv.setOnItemClickListener(onDropdownItemClickListener);
        input.bGo.setOnClickListener(v -> onGoButtonClick());
        input.bBackspace.setOnClickListener(v -> onBackspaceButtonClick());
        input.bPageUp.setOnClickListener(v -> onPageUpButtonClick());
        input.bPageDown.setOnClickListener(v -> onPageDownButtonClick());
        input.rv.setLayoutManager(layoutManager);
        input.rv.setAdapter(new Adapter(this::onMapItemClick, this::onMapItemLongClick));
        input.rv.addOnScrollListener(onMapScrollListener);

        return input.getRoot();
    }

    private void onBackButtonClick() {
        switchToPreviousInputMethod();
    }

    private void onDropdownItemClick(int codepoint) {
        setTextFieldText(codepoint);
        scrollTo(codepoint);
    }

    private void onGoButtonClick() {
        CharSequence text = input.actv.getText();
        int codePoint;
        if (text.length() == 0) {
            return;
        } else if (text.length() == 1
                || text.length() == 2 && Character.isHighSurrogate(text.charAt(0)) && Character.isLowSurrogate(text.charAt(1))) {
            codePoint = Character.codePointAt(text, 0);
            setTextFieldText(codePoint);
        } else {
            try {
                codePoint = Integer.parseUnsignedInt(text.toString(), 0x10);
            } catch (NumberFormatException e) {
                showDialog();
                return;
            }
        }
        scrollTo(codePoint);
    }

    private void onBackspaceButtonClick() {
        if (input.actv.hasFocus()) {
            int selSt = input.actv.getSelectionStart(), selEn = input.actv.getSelectionEnd();
            if (selSt == selEn) {
                if (selSt > 0) {
                    input.actv.getText().delete(selSt - 1, selEn);
                }
            } else {
                input.actv.getText().delete(selSt, selEn);
            }
        } else {
            getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        }
    }

    private void onPageUpButtonClick() {
        setTextFieldText(position - 0x100);
        scrollTo(position - 0x100);
    }

    private void onPageDownButtonClick() {
        setTextFieldText(position + 0x100);
        scrollTo(position + 0x100);
    }

    private void onMapItemClick(int codePoint) {
        String text = String.valueOf(Character.toChars(codePoint));
        if (input.actv.hasFocus()) {
            int selSt = input.actv.getSelectionStart(), selEn = input.actv.getSelectionEnd();
            if (selSt != selEn) {
                input.actv.getText().delete(selSt, selEn);
            }
            input.actv.getText().insert(selSt, text);
        } else {
            getCurrentInputConnection().commitText(String.valueOf(Character.toChars(codePoint)), 1);
            setTextFieldText(codePoint);
        }
    }

    private boolean onMapItemLongClick(int codepoint) {
        if (input.actv.hasFocus()) {
            return false;
        }
        InputConnection ic = getCurrentInputConnection();
        CharSequence text = ic.getTextBeforeCursor(2, 0);
        if (text.length() == 0) {
            return false;
        }
        int codepointFrom;
        if (Character.isHighSurrogate(text.charAt(0)) && text.length() == 2 && Character.isLowSurrogate(text.charAt(1))) {
            codepointFrom = Character.toCodePoint(text.charAt(0), text.charAt(1));
        } else {
            codepointFrom = text.charAt(text.length() - 1);
        }
        if (codepointFrom == codepoint) {
            return false;
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(input.getRoot().getContext(), com.google.android.material.R.style.MaterialAlertDialog_Material3)
                .setTitle(String.format("Commit %d characters", Math.abs(codepoint - codepointFrom)))
                .setPositiveButton("OK", (d, which) -> {
                    StringBuilder sb = new StringBuilder();
                    for (int step = codepointFrom <= codepoint ? 1 : -1, i = codepointFrom + step; i != codepoint; i += step) {
                        sb.append(Character.toChars(i));
                    }
                    sb.append(Character.toChars(codepoint));
                    ic.commitText(sb.toString(), 1);
                })
                .setNegativeButton("Cancel", null)
                .create();
        Window window = dialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.gravity = Gravity.BOTTOM;
        window.setAttributes(layoutParams);
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.show();
        setTextFieldText(codepoint);
        return true;
    }

    private void onMapScroll(int dy) {
        position = layoutManager.findFirstVisibleItemPosition();
        if (dy != 0 && Math.abs(dy) < 0x100) {
            input.actv.setText(Integer.toHexString(position).toUpperCase());
        }
    }

    private void scrollTo(int codePoint) {
        layoutManager.scrollToPositionWithOffset(codePoint, 0);
        input.actv.clearFocus();
    }

    private void setTextFieldText(int codePoint) {
        input.actv.setText(Integer.toHexString(codePoint).toUpperCase());
    }

    private void showDialog() {
        CharSequence text = input.actv.getText();
        List<CharSequence> itemList = new ArrayList<>();
        List<Integer> codePointList = new ArrayList<>();
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            CharSequence item;
            int codePoint;
            if (Character.isHighSurrogate(ch) && i < text.length() - 1 && Character.isLowSurrogate(text.charAt(i + 1))) {
                char ch2 = text.charAt(++i);
                codePoint = Character.toCodePoint(ch, ch2);
                item = String.format("U+%-6s %c%c", Integer.toHexString(codePoint).toUpperCase(), ch, ch2);
            } else {
                codePoint = ch;
                item = String.format("U+%-6s %c", Integer.toHexString(codePoint).toUpperCase(), ch);
            }
            codePointList.add(codePoint);
            itemList.add(item);
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(input.getRoot().getContext(), com.google.android.material.R.style.MaterialAlertDialog_Material3)
                .setTitle("Go to")
                .setItems(itemList.toArray(new CharSequence[0]), (d, which) -> {
                    setTextFieldText(codePointList.get(which));
                    scrollTo(codePointList.get(which));
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.show();
    }
}
