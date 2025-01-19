package com.example.kiparys.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.provider.OpenableColumns
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random


object ImageUtil {

    suspend fun generatePlaceholderImage(firstLetter: Char, width: Int, height: Int): Bitmap {
        return withContext(Dispatchers.Default) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawBackgroundColor(canvas, width, height)
            drawCenteredText(canvas, firstLetter, width)

            bitmap
        }
    }

    suspend fun generatePlaceholderImageWithGradient(
        firstLetter: Char,
        width: Int,
        height: Int
    ): Bitmap {
        return withContext(Dispatchers.Default) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawGradientBackground(canvas, width, height)
            drawCenteredText(canvas, firstLetter, width)

            bitmap
        }
    }

    private fun drawBackgroundColor(canvas: Canvas, width: Int, height: Int) {
        val random = Random.Default
        val backgroundColor =
            Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
            color = backgroundColor
        })
    }

    private fun drawGradientBackground(canvas: Canvas, width: Int, height: Int) {
        val random = Random.Default

        val deepPastelColors = listOf(
            Color.rgb(200, 100, 120),
            Color.rgb(70, 130, 180),
            Color.rgb(60, 179, 113),
            Color.rgb(210, 140, 80),
            Color.rgb(138, 43, 226),
            Color.rgb(255, 160, 122),
            Color.rgb(123, 104, 238),
            Color.rgb(147, 112, 219),
            Color.rgb(255, 99, 71),
            Color.rgb(100, 149, 237),
            Color.rgb(72, 209, 204),
            Color.rgb(244, 164, 96)
        )

        val startColor = deepPastelColors[random.nextInt(deepPastelColors.size)]
        val endColor = deepPastelColors[random.nextInt(deepPastelColors.size)]

        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                startColor, endColor,
                Shader.TileMode.CLAMP
            )
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)
    }

    private fun drawCenteredText(canvas: Canvas, firstLetter: Char, width: Int) {
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = (width / 2).toFloat()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val xPos = canvas.width / 2
        val yPos = (canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2).toInt()
        canvas.drawText(firstLetter.toString(), xPos.toFloat(), yPos.toFloat(), textPaint)
    }

    fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
        cursor?.moveToFirst()
        val fileSize = sizeIndex?.let { cursor.getLong(it) } ?: 0L
        cursor?.close()
        return fileSize
    }

    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream.use {
                return BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateQrCode(content: String, size: Int): Bitmap {
        val writer = QRCodeWriter()
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) -0x1000000 else -0x1)
            }
        }
        return bmp
    }

}
