package com.pankaj.timeinputedittext;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

/**
 * A custom {@link AppCompatTextView} which works like EditText, and it handles time into format of HH:mm:ss.
 * Current validation will not allow you to enter hour more than 23, minutes not more than 59 and
 * seconds not more than 59.
 *
 * <pre>
 * Below features it can do
 *
 * 1. Value will never be empty. It will show current set time by using {@link TimeHMSEditText#setHour(int)},
 *      {@link TimeHMSEditText#setMinutes(int)} and {@link TimeHMSEditText#setSeconds(int)}. If not set, it will show 00:00:00
 *
 * 2. BACK and SPACE keys are used to move cursor
 *      2.1. BACK key will move cursor backward
 *      2.2. SPACE key will move cursor forward
 *
 * 3. It will highlight current selected digit and if any valid number pressed using keyboard, that selected digit will
 *      be replaced by newly entered digit.
 *
 * 4. It supports Virtual and Soft Keyboards both.
 * </pre>
 *
 * Created by Pankaj Kumar on 07/06/18.
 * EAT | DRINK | CODE
 */
public class TimeHMSEditText extends AppCompatTextView {

    // Default position set to -1.
    private static final int POSITION_NONE = -1;

    private int[] digits = new int[6];
    private int currentPosition = POSITION_NONE;
    private int mImeOptions;

    public TimeHMSEditText(Context context) {
        this(context, null, 0);
    }

    public TimeHMSEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimeHMSEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusableInTouchMode(true);

        if (attrs != null && !isInEditMode()) {
            mImeOptions = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "imeOptions", 0);
        }

        updateText();
    }

    /**
     * @return the current hour (from 0 to 23)
     */
    public int getHour() {
        return digits[0] * 10 + digits[1];
    }

    /**
     * @return the current minute
     */
    public int getMinutes() {
        return digits[2] * 10 + digits[3];
    }

    /**
     * @return the current second
     */
    public int getSeconds() {
        return digits[4] * 10 + digits[5];
    }

    /**
     * Set the current hour
     *
     * @param hour hour (from 0 to 23)
     */
    public void setHour(int hour) {
        hour = hour % 24;
        digits[0] = hour / 10;
        digits[1] = hour % 10;
        updateText();
    }

    /**
     * Set the current minute
     *
     * @param min minutes (from 0 to 59)
     */
    public void setMinutes(int min) {
        min = min % 60;
        digits[2] = min / 10;
        digits[3] = min % 10;
        updateText();
    }

    /**
     * Set the current seconds
     *
     * @param seconds seconds (from 0 to 59)
     */
    public void setSeconds(int seconds) {
        seconds = seconds % 60;
        digits[4] = seconds / 10;
        digits[5] = seconds % 10;
        updateText();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        // hide cursor if not focused
        currentPosition = focused ? 0 : POSITION_NONE;
        updateText();
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    private void updateText() {
        // Calculate position to make current text as bold
        int bold = 0;
        if (currentPosition > 3) {
            bold = currentPosition + 2;
        } else if (currentPosition > 1) {
            bold = currentPosition + 1;
        } else {
            bold = currentPosition;
        }

        int color = getTextColors().getDefaultColor();
        Spannable text = new SpannableString(String.format("%02d:%02d:%02d", getHour(), getMinutes(), getSeconds()));
        if (bold >= 0) {
            text.setSpan(new ForegroundColorSpan(color & 0xFFFFFF | 0xA0000000), 0, 5,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new StyleSpan(Typeface.BOLD), bold, bold + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new ForegroundColorSpan(Color.BLACK), bold, bold + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new BackgroundColorSpan(0x40808080), bold, bold + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        setText(text);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            requestFocusFromTouch();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(this, 0);
            if (currentPosition == POSITION_NONE) {
                currentPosition = 0;
                updateText();
            }
        }
        return true;
    }

    private boolean onKeyEvent(int keyCode, KeyEvent event) {
        if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        if (keyCode == KeyEvent.KEYCODE_DEL) {
            // moves cursor backward
            currentPosition = currentPosition >= 0 ? (currentPosition + 5) % 6 : 5;

            updateText();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            // moves cursor forward
            currentPosition = (currentPosition + 1) % 6;
            updateText();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            View v = focusSearch(FOCUS_DOWN);
            boolean next = v != null;
            if (next) {
                next = v.requestFocus(FOCUS_DOWN);
            }
            if (!next) {
                hideKeyboard();
                currentPosition = POSITION_NONE;
                updateText();
            }
            return true;
        }

        char c = (char) event.getUnicodeChar();
        if (c >= '0' && c <= '9') {
            currentPosition = currentPosition == POSITION_NONE ? 0 : currentPosition;
            int n = c - '0';
            boolean valid = false;

            switch (currentPosition) {
                case 0:    // first hour digit must be 0-2
                    valid = n <= 2;
                    break;
                case 1:    // second hour digit must be 0-3 if first digit is 2
                    valid = digits[0] < 2 || n <= 3;
                    break;
                case 2:    // first minute digit must be 0-6
                    valid = n < 6;
                    break;
                case 3:    // second minute digit always valid (0-9)
                    valid = true;
                    break;
                case 4:    // first seconds digit must be 0-6
                    valid = n < 6;
                    break;
                case 5:    // second seconds digit always valid (0-9)
                    valid = true;
                    break;
            }

            if (valid) {
                if (currentPosition == 0 && n == 2 && digits[1] > 3) { // clip to 23 hours max
                    digits[1] = 3;
                }

                digits[currentPosition] = n;
                currentPosition = currentPosition < 5 ? currentPosition + 1
                        : POSITION_NONE;    // if it is the last digit, hide cursor
                updateText();
            }

            return true;
        }

        return false;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // events from physical keyboard
        return onKeyEvent(keyCode, event);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        // manage events from the virtual keyboard
        outAttrs.actionLabel = null;
        outAttrs.label = "time";
        outAttrs.inputType = InputType.TYPE_CLASS_NUMBER;
        outAttrs.imeOptions = mImeOptions | EditorInfo.IME_FLAG_NO_EXTRACT_UI;

        if ((outAttrs.imeOptions & EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_UNSPECIFIED) {
            if (focusSearch(FOCUS_DOWN) != null) {
                outAttrs.imeOptions |= EditorInfo.IME_ACTION_NEXT;
            } else {
                outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
            }
        }

        return new BaseInputConnection(this, false) {
            @Override
            public boolean performEditorAction(int actionCode) {
                if (actionCode == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard();
                    currentPosition = POSITION_NONE;
                    updateText();
                } else if (actionCode == EditorInfo.IME_ACTION_NEXT) {
                    View v = focusSearch(FOCUS_DOWN);
                    if (v != null) {
                        v.requestFocus(FOCUS_DOWN);
                    }
                }
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                onKeyEvent(KeyEvent.KEYCODE_DEL, null);
                return true;
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                onKeyEvent(event.getKeyCode(), event);
                return true;
            }
        };
    }
}