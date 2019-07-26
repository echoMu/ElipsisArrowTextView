package com.echomu.elipsisarrowtextview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatTextView;
import android.text.DynamicLayout;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.lang.reflect.Field;

public class ElipsisArrowTextView extends AppCompatTextView {

    private static final String CLASS_NAME_VIEW = "android.view.View";
    private static final String CLASS_NAME_LISTENER_INFO = "android.view.View$ListenerInfo";
    private static final String ELLIPSIS_HINT = "...";
    private static final String GAP_TO_EXPAND_HINT = " ";
    private static final String GAP_TO_SHRINK_HINT = " ";
    private static final int MAX_LINES_ON_SHRINK = 3;
    private static final int TO_EXPAND_HINT_COLOR = 0xFFFF98BE;
    private static final int TO_SHRINK_HINT_COLOR = 0xFFFF98BE;
    private static final int TO_EXPAND_HINT_COLOR_BG_PRESSED = 0x55999999;
    private static final int TO_SHRINK_HINT_COLOR_BG_PRESSED = 0x55999999;
    private static final boolean SHOW_TO_EXPAND_HINT = true;
    private static final boolean SHOW_TO_SHRINK_HINT = true;

    private String mEllipsisHint;
    private String mGapToExpandHint = GAP_TO_EXPAND_HINT;
    private String mGapToShrinkHint = GAP_TO_SHRINK_HINT;
    private boolean mShowToExpandHint = SHOW_TO_EXPAND_HINT;
    private boolean mShowToShrinkHint = SHOW_TO_SHRINK_HINT;
    private int mMaxLinesOnShrink = MAX_LINES_ON_SHRINK;
    private int mToExpandHintColor = TO_EXPAND_HINT_COLOR;
    private int mToShrinkHintColor = TO_SHRINK_HINT_COLOR;
    private int mToExpandHintColorBgPressed = TO_EXPAND_HINT_COLOR_BG_PRESSED;
    private int mToShrinkHintColorBgPressed = TO_SHRINK_HINT_COLOR_BG_PRESSED;

    private TextView.BufferType mBufferType = TextView.BufferType.NORMAL;
    private TextPaint mTextPaint;
    private Layout mLayout;
    private int mTextLineCount = -1;
    private int mLayoutWidth = 0;
    private int mFutureTextViewWidth = 0;

    //  the original text of this view
    private CharSequence mOrigText;

    private Bitmap bitmap1;
    private CenteredImageSpan imgSpan1;

    public ElipsisArrowTextView(Context context) {
        super(context);
        init(context);
    }

    public ElipsisArrowTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(context, attrs);
        init(context);
    }

    public ElipsisArrowTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context, attrs);
        init(context);
    }

    private void initAttr(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ElipsisArrowTextView);
        if (a == null) {
            return;
        }
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.ElipsisArrowTextView_arr_MaxLinesOnShrink) {
                mMaxLinesOnShrink = a.getInteger(attr, MAX_LINES_ON_SHRINK);
            } else if (attr == R.styleable.ElipsisArrowTextView_arr_EllipsisHint) {
                mEllipsisHint = a.getString(attr);
            } else if (attr == R.styleable.ElipsisArrowTextView_arr_ToExpandHintShow) {
                mShowToExpandHint = a.getBoolean(attr, SHOW_TO_EXPAND_HINT);
            } else if (attr == R.styleable.ElipsisArrowTextView_arr_ToShrinkHintShow) {
                mShowToShrinkHint = a.getBoolean(attr, SHOW_TO_SHRINK_HINT);
            } else if (attr == R.styleable.ElipsisArrowTextView_arr_ToExpandHintColor) {
                mToExpandHintColor = a.getInteger(attr, TO_EXPAND_HINT_COLOR);
            } else if (attr == R.styleable.ElipsisArrowTextView_arr_ToShrinkHintColor) {
                mToShrinkHintColor = a.getInteger(attr, TO_SHRINK_HINT_COLOR);
            } else if (attr == R.styleable.ElipsisArrowTextView_arr_ToExpandHintColorBgPressed) {
                mToExpandHintColorBgPressed = a.getInteger(attr, TO_EXPAND_HINT_COLOR_BG_PRESSED);
            } else if (attr == R.styleable.ElipsisArrowTextView_arr_ToShrinkHintColorBgPressed) {
                mToShrinkHintColorBgPressed = a.getInteger(attr, TO_SHRINK_HINT_COLOR_BG_PRESSED);
            } else if (attr == R.styleable.ElipsisArrowTextView_arr_GapToExpandHint) {
                mGapToExpandHint = a.getString(attr);
            } else if (attr == R.styleable.ElipsisArrowTextView_arr_GapToShrinkHint) {
                mGapToShrinkHint = a.getString(attr);
            }
        }
        a.recycle();
    }

    private void init(Context context) {
        bitmap1 = BitmapFactory.decodeResource(getResources(), R.mipmap.more);
        imgSpan1 = new CenteredImageSpan(getContext(), R.mipmap.more);

        if (TextUtils.isEmpty(mEllipsisHint)) {
            mEllipsisHint = ELLIPSIS_HINT;
        }
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver obs = getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);
                setTextInternal(getNewTextByConfig(), mBufferType);
            }
        });
    }

    /**
     * used in ListView or RecyclerView to update ExpandableTextView
     *
     * @param text                original text
     * @param futureTextViewWidth the width of ExpandableTextView in px unit,
     *                            used to get max line number of original text by given the width
     */
    public void updateForRecyclerView(CharSequence text, int futureTextViewWidth) {
        mFutureTextViewWidth = futureTextViewWidth;
        setText(text);
    }

    public void updateForRecyclerView(CharSequence text, TextView.BufferType type, int futureTextViewWidth) {
        mFutureTextViewWidth = futureTextViewWidth;
        setText(text, type);
    }

    public void setMaxLinesOnShrink(CharSequence text, int mMaxLinesOnShrink) {
        this.mMaxLinesOnShrink = mMaxLinesOnShrink;
        setText(text);
    }

    /**
     * refresh and get a will-be-displayed text by current configuration
     *
     * @return get a will-be-displayed text
     */
    private CharSequence getNewTextByConfig() {
        if (TextUtils.isEmpty(mOrigText)) {
            return mOrigText;
        }

        mLayout = getLayout();
        if (mLayout != null) {
            mLayoutWidth = mLayout.getWidth();
        }

        if (mLayoutWidth <= 0) {
            if (getWidth() == 0) {
                if (mFutureTextViewWidth == 0) {
                    return mOrigText;
                } else {
                    mLayoutWidth = mFutureTextViewWidth - getPaddingLeft() - getPaddingRight();
                }
            } else {
                mLayoutWidth = getWidth() - getPaddingLeft() - getPaddingRight();
            }
        }

        mTextPaint = getPaint();

        mTextLineCount = -1;
        mLayout = null;
        mLayout = new DynamicLayout(mOrigText, mTextPaint, mLayoutWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        mTextLineCount = mLayout.getLineCount();

        if (mTextLineCount <= mMaxLinesOnShrink) {
            return mOrigText;
        }

        int indexEnd = getValidLayout().getLineEnd(mMaxLinesOnShrink - 1);
        int indexStart = getValidLayout().getLineStart(mMaxLinesOnShrink - 1);
        int indexEndTrimmed = indexEnd
                - getLengthOfString(mEllipsisHint)
                - (mShowToExpandHint ? getLengthOfString(mGapToExpandHint) : 0);

        if (indexEndTrimmed <= indexStart) {
            indexEndTrimmed = indexEnd;
        }

        int remainWidth = getValidLayout().getWidth() -
                (int) (mTextPaint.measureText(mOrigText.subSequence(indexStart, indexEndTrimmed).toString()) + 0.5) - bitmap1.getWidth();
        float widthTailReplaced = mTextPaint.measureText(getContentOfString(mEllipsisHint)
                + (mShowToExpandHint ? (getContentOfString(mGapToExpandHint)) : ""));

        int indexEndTrimmedRevised = indexEndTrimmed;
        if (remainWidth > widthTailReplaced) {
            int extraOffset = 0;
            int extraWidth = 0;
            while (remainWidth > widthTailReplaced + extraWidth) {
                extraOffset++;
                if (indexEndTrimmed + extraOffset <= mOrigText.length()) {
                    extraWidth = (int) (mTextPaint.measureText(
                            mOrigText.subSequence(indexEndTrimmed, indexEndTrimmed + extraOffset).toString()) + 0.5);
                } else {
                    break;
                }
            }
            indexEndTrimmedRevised += extraOffset - 1;
        } else {
            int extraOffset = 0;
            int extraWidth = 0;
            while (remainWidth + extraWidth < widthTailReplaced) {
                extraOffset--;
                if (indexEndTrimmed + extraOffset > indexStart) {
                    extraWidth = (int) (mTextPaint.measureText(mOrigText.subSequence(indexEndTrimmed + extraOffset, indexEndTrimmed).toString()) + 0.5);
                } else {
                    break;
                }
            }
            indexEndTrimmedRevised += extraOffset;
        }

        String fixText = removeEndLineBreak(mOrigText.subSequence(0, indexEndTrimmedRevised));
        SpannableStringBuilder ssbShrink = new SpannableStringBuilder(fixText);

        ssbShrink.append(mEllipsisHint);

        if (mShowToExpandHint) {

            if (issetSpecialColor) {
                int lenth = ssbShrink.length();
                if (specialColorLenth <= lenth) {
                    lenth = specialColorLenth;
                }
                ssbShrink.setSpan(colorSpan, specialColorStart, lenth, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            ssbShrink.append(getContentOfString(mGapToExpandHint));

            ssbShrink.append("+");
            ssbShrink.setSpan(imgSpan1, ssbShrink.length() - 1, ssbShrink.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ssbShrink;
    }

    private String removeEndLineBreak(CharSequence text) {
        String str = text.toString();
        while (str.endsWith("\n")) {
            str = str.substring(0, str.length() - 1);
        }


        Layout mLayout = new DynamicLayout(str, mTextPaint, mLayoutWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        if (mLayout.getLineCount() > mMaxLinesOnShrink) {
            if (str.contains("\n")) {
                str = str.substring(0, str.lastIndexOf("\n"));
            }
        }

        return str;
    }

    private Layout getValidLayout() {
        return mLayout != null ? mLayout : getLayout();
    }

    @Override
    public void setText(CharSequence text, TextView.BufferType type) {
        mOrigText = text;
        mBufferType = type;
        setTextInternal(getNewTextByConfig(), type);
    }

    private void setTextInternal(CharSequence text, TextView.BufferType type) {

        super.setText(text, type);
    }

    private int getLengthOfString(String string) {
        if (string == null) {
            return 0;
        }
        return string.length();
    }

    private String getContentOfString(String string) {
        if (string == null) {
            return "";
        }
        return string;
    }

    public View.OnClickListener getOnClickListener(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return getOnClickListenerV14(view);
        } else {
            return getOnClickListenerV(view);
        }
    }

    private View.OnClickListener getOnClickListenerV(View view) {
        View.OnClickListener retrievedListener = null;
        try {
            Field field = Class.forName(CLASS_NAME_VIEW).getDeclaredField("mOnClickListener");
            field.setAccessible(true);
            retrievedListener = (View.OnClickListener) field.get(view);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retrievedListener;
    }

    private View.OnClickListener getOnClickListenerV14(View view) {
        View.OnClickListener retrievedListener = null;
        try {
            Field listenerField = Class.forName(CLASS_NAME_VIEW).getDeclaredField("mListenerInfo");
            Object listenerInfo = null;

            if (listenerField != null) {
                listenerField.setAccessible(true);
                listenerInfo = listenerField.get(view);
            }

            Field clickListenerField = Class.forName(CLASS_NAME_LISTENER_INFO).getDeclaredField("mOnClickListener");

            if (clickListenerField != null && listenerInfo != null) {
                clickListenerField.setAccessible(true);
                retrievedListener = (View.OnClickListener) clickListenerField.get(listenerInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retrievedListener;
    }

    boolean issetSpecialColor = false;//是否有不同字体需要变颜色
    int specialColorStart;//开始变色的位置
    int specialColorLenth;//需要变色的长度
    ForegroundColorSpan colorSpan;

    public void setSpecialColor(int start, int lenth, ForegroundColorSpan colorSpan) {
        specialColorStart = start;
        specialColorLenth = lenth;
        this.colorSpan = colorSpan;
        issetSpecialColor = true;
    }

    class CenteredImageSpan extends ImageSpan {

        public CenteredImageSpan(Context context, final int drawableRes) {
            super(context, drawableRes);
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text,
                         int start, int end, float x,
                         int top, int y, int bottom, @NonNull Paint paint) {
            // image to draw
            Drawable b = getDrawable();
            // font metrics of text to be replaced
            Paint.FontMetricsInt fm = paint.getFontMetricsInt();
            int transY = (y + fm.descent + y + fm.ascent) / 2
                    - b.getBounds().bottom / 2;

            canvas.save();
            canvas.translate(x, transY);
            b.draw(canvas);
            canvas.restore();
        }
    }

}