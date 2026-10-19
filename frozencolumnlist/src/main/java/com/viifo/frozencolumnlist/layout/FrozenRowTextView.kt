package com.viifo.frozencolumnlist.layout

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.viifo.frozencolumnlist.data.FrozenTextColumn

/** One scrolling region containing any number of logical text columns. Created by FrozenTextRowLayout. */
@SuppressLint("ViewConstructor") // Regions require a row host and are created programmatically.
class FrozenRowTextView internal constructor(
    context: Context,
    private val row: FrozenTextRowLayout
) : View(context) {
    internal var fromColumn = 0
    internal var untilColumn = 0
    private val cells = mutableMapOf<Int, Cell>()
    private val paint = Paint()
    private val bounds = Rect()
    private var localOrigin = 0
    internal var desiredTextHeight = 0
        private set

    private data class Cell(
        val column: FrozenTextColumn,
        val width: Int,
        val direction: Int,
        val layout: StaticLayout,
        val icon: Drawable?,
        val textLeft: Int,
        val iconLeft: Int
    )

    private val accessibility = object : ExploreByTouchHelper(this) {
        override fun getVirtualViewAt(x: Float, y: Float): Int =
            row.columnAt(x + left, y + top)?.takeIf { it in fromColumn until untilColumn }
                ?: INVALID_ID

        override fun getVisibleVirtualViews(ids: MutableList<Int>) {
            for (index in fromColumn until untilColumn) {
                if (row.getColumnBounds(index, bounds)) ids.add(index)
            }
        }

        @Suppress("DEPRECATION") // ExploreByTouchHelper derives screen bounds from parent bounds.
        override fun onPopulateNodeForVirtualView(id: Int, node: AccessibilityNodeInfoCompat) {
            val column = row.columns.getOrNull(id)
            node.text = column?.contentDescription ?: column?.let {
                listOfNotNull(it.text, it.secondaryText).joinToString(" ")
            }.orEmpty()
            node.className = "android.widget.TextView"
            node.isClickable = true
            node.isFocusable = true
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
            row.getColumnBounds(id, bounds)
            bounds.offset(-left, -top)
            node.setBoundsInParent(bounds)
        }

        override fun onPerformActionForVirtualView(id: Int, action: Int, args: Bundle?): Boolean {
            if (action != AccessibilityNodeInfoCompat.ACTION_CLICK) return false
            if (!row.activateColumn(id)) return false
            sendEventForVirtualView(id, AccessibilityEvent.TYPE_VIEW_CLICKED)
            return true
        }
    }

    init {
        ViewCompat.setAccessibilityDelegate(this, accessibility)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
    }

    internal fun prepare(geometry: TextColumnGeometry) {
        cells.keys.retainAll((fromColumn until untilColumn).toSet())
        desiredTextHeight = 0
        localOrigin = if (fromColumn == 0) geometry.paddingLeft else geometry.frozenEnd
        for (index in fromColumn until untilColumn) {
            val column = row.columns[index]
            val width = geometry.width(index)
            val previous = cells[index]
            val cell = if (previous?.column == column && previous.width == width &&
                previous.direction == layoutDirection) previous else createCell(column, width)
            cells[index] = cell
            desiredTextHeight = maxOf(desiredTextHeight, cell.layout.height, cell.icon?.intrinsicHeight ?: 0)
        }
        accessibility.invalidateRoot()
        invalidate()
    }

    private fun createCell(column: FrozenTextColumn, width: Int): Cell {
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val icon = column.endDrawable.takeIf { it != 0 }?.let {
            AppCompatResources.getDrawable(context, it)?.mutate()
        }
        val iconWidth = icon?.intrinsicWidth?.coerceAtLeast(0) ?: 0
        val iconSpace = if (icon == null) 0 else iconWidth + column.drawablePadding
        val textWidth = (width - column.paddingStart - column.paddingEnd - iconSpace).coerceAtLeast(1)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = column.textColor
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, column.textSizeSp, resources.displayMetrics
            )
            typeface = if (column.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val absoluteGravity = Gravity.getAbsoluteGravity(column.gravity, layoutDirection) and
            Gravity.HORIZONTAL_GRAVITY_MASK
        val alignment = when (absoluteGravity) {
            Gravity.CENTER_HORIZONTAL -> Layout.Alignment.ALIGN_CENTER
            Gravity.RIGHT -> if (rtl) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE
            else -> if (rtl) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
        }
        val text = column.styledText(context)
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, textWidth)
            .setAlignment(alignment)
            .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
            .setIncludePad(true)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(if (column.secondaryText == null) 1 else 2)
            .build()
        return Cell(
            column, width, layoutDirection, layout, icon,
            if (rtl) column.paddingEnd + iconSpace else column.paddingStart,
            if (rtl) column.paddingEnd else width - column.paddingEnd - iconWidth
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val geometry = row.geometry ?: return
        // The row allows native frozen-column shadows outside child bounds. Text regions must
        // explicitly clip themselves, otherwise a partially visible cell paints over the frozen View.
        val regionSave = canvas.save()
        canvas.clipRect(0, 0, width, height)
        for (index in fromColumn until untilColumn) {
            val cell = cells[index] ?: continue
            val x = geometry.left(index, row.leftOffset, row.rightOffset) - localOrigin
            if (x >= width) break
            if (x + cell.width <= 0) continue
            val save = canvas.save()
            canvas.clipRect(x, 0, x + cell.width, height)
            paint.color = cell.column.backgroundColor
            canvas.drawRect(x.toFloat(), 0f, (x + cell.width).toFloat(), height.toFloat(), paint)
            val y = when (cell.column.gravity and Gravity.VERTICAL_GRAVITY_MASK) {
                Gravity.TOP -> 0
                Gravity.BOTTOM -> height - cell.layout.height
                else -> (height - cell.layout.height) / 2
            }
            cell.icon?.let { icon ->
                val iconTop = (height - icon.intrinsicHeight) / 2
                icon.setBounds(x + cell.iconLeft, iconTop, x + cell.iconLeft + icon.intrinsicWidth,
                    iconTop + icon.intrinsicHeight)
                icon.draw(canvas)
            }
            canvas.translate((x + cell.textLeft).toFloat(), y.toFloat())
            cell.layout.draw(canvas)
            canvas.restoreToCount(save)
        }
        canvas.restoreToCount(regionSave)
    }

    internal fun offsetsChanged() {
        invalidate()
        accessibility.invalidateRoot()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        cells.clear()
        requestLayout()
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean =
        accessibility.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        accessibility.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        accessibility.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }
}
