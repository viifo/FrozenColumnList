package com.viifo.frozencolumnlist.layout

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.widget.TextView
import com.viifo.frozencolumnlist.data.FrozenTextColumn

internal fun FrozenTextColumn.styledText(context: Context): CharSequence {
    val secondary = secondaryText ?: return text
    return SpannableString("$text\n$secondary").apply {
        val start = text.length + 1
        setSpan(ForegroundColorSpan(secondaryTextColor), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, secondaryTextSizeSp, context.resources.displayMetrics
        ).toInt()
        setSpan(AbsoluteSizeSpan(size), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

internal fun TextView.bindTextColumn(column: FrozenTextColumn) {
    id = column.id
    text = column.styledText(context)
    contentDescription = column.contentDescription
    setTextColor(column.textColor)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, column.textSizeSp)
    setTypeface(Typeface.DEFAULT, if (column.bold) Typeface.BOLD else Typeface.NORMAL)
    gravity = column.gravity
    setPaddingRelative(column.paddingStart, 0, column.paddingEnd, 0)
    setBackgroundColor(column.backgroundColor)
    maxLines = if (column.secondaryText == null) 1 else 2
    ellipsize = TextUtils.TruncateAt.END
    setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, column.endDrawable, 0)
    compoundDrawablePadding = column.drawablePadding
}
