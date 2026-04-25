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

class MapMarkerRenderer(private val context: Context) {

    fun render(text: String, hasData: Boolean): Drawable {
        val bgColor = if (hasData) ACTIVE_COLOR else INACTIVE_COLOR

        val markerView = TextView(context).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            maxLines = 1
            typeface = Typeface.DEFAULT_BOLD
            setPadding(32, 20, 32, 20)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32f
                setColor(bgColor)
            }
        }

        markerView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

        val bitmap = createBitmap(markerView.measuredWidth, markerView.measuredHeight)
        markerView.draw(Canvas(bitmap))

        return bitmap.toDrawable(context.resources)
    }

    companion object {
        private const val ACTIVE_COLOR = 0xFF1976D2.toInt()
        private const val INACTIVE_COLOR = 0xFF78909C.toInt()
    }
}
