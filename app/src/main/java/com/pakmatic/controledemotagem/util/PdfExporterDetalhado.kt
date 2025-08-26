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

    fun gerarPdf(context: Context, outputStream: OutputStream, apontamentos: List<ApontamentoCompleto>) {
        val document = PdfDocument()

        apontamentos.forEachIndexed { index, apontamento ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()

            // <<< CORREÇÃO 1: MUDANÇA DE 'val' PARA 'var' >>>
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
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
            yPosition += 12
            canvas.drawText(apontamento.apontamento.item, MARGIN, yPosition, paintBody)
            yPosition += 20

            canvas.drawText("RESPONSÁVEL:", MARGIN, yPosition, paintHeader)
            yPosition += 12
            canvas.drawText(apontamento.apontamento.nomeResponsavel, MARGIN, yPosition, paintBody)

            canvas.drawText("DATA:", PAGE_WIDTH - MARGIN - 80, yPosition - 10, paintHeader)
            canvas.drawText(dataFormatada, PAGE_WIDTH - MARGIN - 80, yPosition, paintBody)
            yPosition += 30

            apontamento.fases.forEach { fase ->
                // <<< CORREÇÃO 2: LÓGICA DE NOVA PÁGINA AJUSTADA >>>
                if (yPosition > PAGE_HEIGHT - 100) {
                    document.finishPage(page)
                    val newPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create()
                    page = document.startPage(newPageInfo)
                    canvas = page.canvas
                    yPosition = MARGIN
                }

                canvas.drawLine(MARGIN, yPosition - 5, PAGE_WIDTH - MARGIN, yPosition - 5, paintLine)
                val primeiraFotoUri = fase.caminhosFotos.firstOrNull()
                var textX = MARGIN
                var fotoHeight = 0f
                if (primeiraFotoUri != null) {
                    try {
                        val fotoStream = context.contentResolver.openInputStream(Uri.parse(primeiraFotoUri))
                        val fotoBitmap = BitmapFactory.decodeStream(fotoStream)
                        val scaledFoto = Bitmap.createScaledBitmap(fotoBitmap, 80, 80, true)
                        canvas.drawBitmap(scaledFoto, MARGIN, yPosition, null)
                        fotoStream?.close()
                        textX = MARGIN + 95
                        fotoHeight = 85f
                    } catch (e: Exception) { /* Ignora foto corrompida */ }
                }
                canvas.drawText("Fase:", textX, yPosition + 10, paintHeader)
                var textY = yPosition + 22
                val descricao = fase.descricao
                var start = 0
                while (start < descricao.length) {
                    val end = paintBody.breakText(descricao, start, descricao.length, true, PAGE_WIDTH - textX - MARGIN, null)
                    canvas.drawText(descricao, start, start + end, textX, textY, paintBody)
                    start += end
                    textY += 12
                }
                yPosition += Math.max(fotoHeight, textY - yPosition + 10)
            }

            document.finishPage(page)
        }

        try {
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }
}