package com.pakmatic.controledemotagem.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.pakmatic.controledemotagem.R
import com.pakmatic.controledemotagem.data.local.ApontamentoCompleto // <<< CORREÇÃO 1 AQUI
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object PdfExporter {

    private const val PAGE_WIDTH = 595 // Largura A4 em pontos
    private const val PAGE_HEIGHT = 842 // Altura A4 em pontos
    private const val MARGIN = 30f

    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null) return "--:--"
        return SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDuration(totalSeconds: Long): String {
        if (totalSeconds < 0) return "0h 0m"
        val hours = TimeUnit.SECONDS.toHours(totalSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
        return String.format("%02dh %02dm", hours, minutes)
    }

    // <<< CORREÇÃO 2 AQUI >>>
    fun gerarPdf(context: Context, outputStream: OutputStream, apontamentos: List<ApontamentoCompleto>) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = MARGIN

        val paintTitle = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.BLACK
        }
        val paintHeader = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8f
            color = Color.BLACK
        }
        val paintBody = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 8f
            color = Color.DKGRAY
        }
        val paintBodyBold = Paint(paintBody).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintLine = Paint().apply {
            strokeWidth = 1f
            color = Color.LTGRAY
        }

        try {
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 85, 25, false)
            canvas.drawBitmap(scaledLogo, MARGIN, y, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        canvas.drawText("Relatório de Montagem", PAGE_WIDTH - MARGIN - 120, y + 20, paintTitle)
        y += 60

        val headerY = y
        canvas.drawLine(MARGIN, headerY + 5, PAGE_WIDTH - MARGIN, headerY + 5, paintLine)
        canvas.drawText("ITEM", MARGIN, headerY, paintHeader)
        canvas.drawText("RESPONSÁVEL", MARGIN + 80, headerY, paintHeader)
        canvas.drawText("INÍCIO", MARGIN + 180, headerY, paintHeader)
        canvas.drawText("FINAL", MARGIN + 260, headerY, paintHeader)
        canvas.drawText("T. PARADO", MARGIN + 340, headerY, paintHeader)
        canvas.drawText("STATUS", MARGIN + 420, headerY, paintHeader)
        canvas.drawText("DESCRIÇÃO", MARGIN + 480, headerY, paintHeader)
        y += 20

        apontamentos.forEach { item ->
            val apontamento = item.apontamento
            canvas.drawLine(MARGIN, y + 5, PAGE_WIDTH - MARGIN, y + 5, paintLine)

            canvas.drawText(apontamento.item.take(18), MARGIN, y, paintBodyBold)
            canvas.drawText(apontamento.nomeResponsavel.take(22), MARGIN + 80, y, paintBody)
            canvas.drawText(formatTimestamp(apontamento.timestampInicio), MARGIN + 180, y, paintBody)
            canvas.drawText(formatTimestamp(apontamento.timestampFinal), MARGIN + 260, y, paintBody)
            canvas.drawText(formatDuration(item.tempoTotalParadoSegundos), MARGIN + 340, y, paintBodyBold)
            canvas.drawText(apontamento.status, MARGIN + 420, y, paintBody)
            canvas.drawText(apontamento.descricaoItem.take(15), MARGIN + 480, y, paintBody)
            y += 15
        }

        document.finishPage(page)

        try {
            document.writeTo(outputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }
    }
}