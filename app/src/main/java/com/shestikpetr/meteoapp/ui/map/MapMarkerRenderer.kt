package com.shestikpetr.meteoapp.ui.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

/**
 * Маркер метеостанции в стиле meteo-web `.stmark`:
 * — небольшая «плашка» с именем и (опционально) значением выбранного параметра справа в моно-шрифте.
 */
class MapMarkerRenderer(private val context: Context) {

    fun render(text: String, active: Boolean, dark: Boolean): Drawable {
        val palette = MapPalette.of(dark)
        val bgColor = palette.bg
        val textColor = if (active) palette.activeText else palette.text
        val borderColor = if (active) palette.accent else palette.border

        val view = TextView(context).apply {
            this.text = text
            textSize = 12f
            setTextColor(textColor)
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 1
            typeface = Typeface.DEFAULT_BOLD
            setPadding(20, 10, 20, 10)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f
                setColor(bgColor)
                setStroke(2, borderColor)
            }
        }
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = createBitmap(view.measuredWidth, view.measuredHeight)
        view.draw(Canvas(bitmap))
        return bitmap.toDrawable(context.resources)
    }

    private data class MapPalette(
        val bg: Int,
        val text: Int,
        val activeText: Int,
        val border: Int,
        val accent: Int
    ) {
        companion object {
            fun of(dark: Boolean): MapPalette = if (dark) MapPalette(
                bg = 0xFF131418.toInt(),
                text = 0xFFECECEF.toInt(),
                activeText = 0xFFB6D2F5.toInt(),
                border = 0xFF2A2C33.toInt(),
                accent = 0xFF66A6F2.toInt()
            ) else MapPalette(
                bg = Color.WHITE,
                text = 0xFF0E0F12.toInt(),
                activeText = 0xFF1F4D8C.toInt(),
                border = 0xFFD8D9DD.toInt(),
                accent = 0xFF2F6BCB.toInt()
            )
        }
    }
}
