假如现在有这样的需求，一段多行的文字，限制最多显示到第几行，末尾以省略号结束，并且是要加上一个图标来辅助，以便用户点击跳转。

![image.png](https://upload-images.jianshu.io/upload_images/817079-b483b6960da0ba3f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

显然，`TextView`并不能轻松地胜任这样的工作。我们需要自定义一个`TextView`来达到这样的效果。
自然而然地，我们会拿`SpannableString`或`SpannableStringBuilder`来实现。

### 一 开始之前
首先，理清实现这个效果需要解决的点在哪里：

1.  原本文字在最后一行显示的最终结束位置

2.  省略号“...”如何加入和计算显示的位置

3.  图标如何加入和计算显示的位置

有了以上的思路，就可以一个个地开发，写代码啦。

### 二 计算文字最后一行结束的位置

关于如何测量文字的有关知识点，立马就想到了大佬[hencoder.com](https://hencoder.com/ui-1-3/)的自定义view系列。

`measureText()`: 它测量的是文字绘制时所占用的宽度。

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

通过以上代码，计算出`indexEndTrimmedRevised`，这个就是我们的文字最终的末尾位置。

### 三 省略号“...”

这个比较好做，我们直接在已经处理过的文本后面加上字符串“...”就可以了。

    ssbShrink.append(mEllipsisHint);

### 四 图标如何加入和计算显示的位置

使用`ImageSpan`，简单的几句代码就可以实现图标一直在最后。

    ssbShrink.append("+");
    ssbShrink.setSpan(imgSpan1, ssbShrink.length() - 1, ssbShrink.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

可是，当我们运行在手机上时，却发现效果跟想象中的不一样。

![image.png](https://upload-images.jianshu.io/upload_images/817079-66bb3a5a3a6fb58a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


什么！！图标竟然和文字不对齐！

细思之下，`ImageSpan`有它自己的对齐方式。

*   DynamicDrawableSpan.ALIGN_BASELINE：以基线对齐

*   DynamicDrawableSpan.ALIGN_BOTTOM：以底部对齐

我们看到的实际效果，就是以上对齐方式的结果。

但是，我们在实际开发中布局的时候，很多时候用到的是垂直居中。

（又默默地温习了大佬的课程...）

为了实现对齐效果，需要先了解一下文字的结构。

先解释一个类：`Paint.FontMetrics`，它表示绘制字体时的度量标准。google的官方api文档对它的字段说明如下：

![image](https://upload-images.jianshu.io/upload_images/817079-f6d2eb5be696cc08.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240) 

其中，ascent: 字体最上端到基线的距离，为负值。

descent：字体最下端到基线的距离，为正值。

如下图

![image](https://upload-images.jianshu.io/upload_images/817079-a7d84ecff01b16a0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240) 

中间那条线就是基线，基线到上面那条线的距离就是ascent，基线到下面那条线的距离就是descent。

因此，我们要让`imagespan`与`text`对齐，只需把`imagespan`放到descent线和ascent线之间的中间位置就可以了。实现方式为重写`ImageSpan`类的draw方法。

最终实现方法如下：

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

解决了以上几点问题之后，最终得到了我们想要的实现效果。虽然这个要实现的效果不复杂，但是也是涉及到了文字的测量、ImageSpan 等几个知识点，认真去对待就会发现还是收获很多。
