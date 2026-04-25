package com.shestikpetr.meteoapp.ui.screens.statistics

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.shestikpetr.meteoapp.data.model.TimeSeriesDataPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun exportDataToCsv(
    context: Context,
    data: List<TimeSeriesDataPoint>,
    stationName: String,
    parameterName: String
) {
    try {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fileFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "${stationName}_${parameterName}_${fileFormatter.format(Date())}.csv"

        val csvContent = buildString {
            appendLine("Дата и время,Значение")
            data.forEach { point ->
                appendLine("${dateFormatter.format(Date(point.time * 1000))},${point.value}")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                }
                Toast.makeText(context, "Файл сохранён в Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            file.writeText(csvContent)
            Toast.makeText(context, "Файл сохранён: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
