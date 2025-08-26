package com.pakmatic.controledemotagem.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.pakmatic.controledemotagem.R
import com.pakmatic.controledemotagem.data.local.ApontamentoCompleto
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporterDetalhado {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun gerarPdf(context: Context, outputStream: OutputStream, apontamento: ApontamentoCompleto) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var yPosition = MARGIN

        val paintTitle = Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 16f; color = Color.BLACK }
        val paintHeader = Paint(paintTitle).apply { textSize = 9f }
        val paintBody = Paint().apply { typeface = Typeface.DEFAULT; textSize = 9f; color = Color.DKGRAY }
        val paintLine = Paint().apply { strokeWidth = 1f; color = Color.LTGRAY }

        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 106, 31, false)
        canvas.drawBitmap(scaledLogo, MARGIN, yPosition, null)
        val dataFormatada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(apontamento.apontamento.timestampInicio))
        canvas.drawText("RELATÓRIO DE FASES", PAGE_WIDTH - MARGIN - 120, yPosition + 15, paintTitle)
        yPosition += 50
        canvas.drawText("EQUIPAMENTO:", MARGIN, yPosition, paintHeader)
        canvas.drawText(apontamento.apontamento.item, MARGIN + 90, yPosition, paintBody)
        canvas.drawText("DATA:", PAGE_WIDTH - MARGIN - 80, yPosition, paintHeader)
        canvas.drawText(dataFormatada, PAGE_WIDTH - MARGIN - 40, yPosition, paintBody)
        yPosition += 15
        canvas.drawText("RESPONSÁVEL:", MARGIN, yPosition, paintHeader)
        canvas.drawText(apontamento.apontamento.nomeResponsavel, MARGIN + 90, yPosition, paintBody)
        yPosition += 30

        apontamento.fases.forEach { fase ->
            canvas.drawLine(MARGIN, yPosition - 5, PAGE_WIDTH - MARGIN, yPosition - 5, paintLine)

            // <<< CORREÇÃO AQUI: Usa o primeiro item da lista de fotos >>>
            val primeiraFotoUri = fase.caminhosFotos.firstOrNull()

            if (primeiraFotoUri != null) {
                try {
                    val fotoStream = context.contentResolver.openInputStream(Uri.parse(primeiraFotoUri))
                    val fotoBitmap = BitmapFactory.decodeStream(fotoStream)
                    val scaledFoto = Bitmap.createScaledBitmap(fotoBitmap, 80, 80, true)
                    canvas.drawBitmap(scaledFoto, MARGIN, yPosition, null)
                    fotoStream?.close()
                } catch (e: Exception) {
                    canvas.drawRect(MARGIN, yPosition, MARGIN + 80, yPosition + 80, paintLine)
                    canvas.drawText("?", MARGIN + 35, yPosition + 45, paintTitle)
                }
            }

            val textX = if (primeiraFotoUri != null) MARGIN + 95 else MARGIN
            canvas.drawText("Fase:", textX, yPosition + 10, paintHeader)
            yPosition += 12
            val descricao = fase.descricao
            var start = 0
            while (start < descricao.length) {
                val end = paintBody.breakText(descricao, start, descricao.length, true, PAGE_WIDTH - textX - MARGIN, null)
                canvas.drawText(descricao, start, start + end, textX, yPosition, paintBody)
                start += end
                yPosition += 12
            }
            yPosition += 10
            if (yPosition > PAGE_HEIGHT - 50) { document.finishPage(page); yPosition = MARGIN }
        }

        document.finishPage(page)
        try { document.writeTo(outputStream) } finally { document.close() }
    }
}