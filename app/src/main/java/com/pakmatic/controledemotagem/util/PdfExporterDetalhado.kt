package com.pakmatic.controledemotagem.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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

    private const val PAGE_WIDTH = 595 // A4 width
    private const val PAGE_HEIGHT = 842 // A4 height
    private const val MARGIN = 40f

    fun gerarPdf(context: Context, outputStream: OutputStream, apontamento: ApontamentoCompleto) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var yPosition = MARGIN

        // --- Estilos de Texto ---
        val paintTitle = Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 16f; color = Color.BLACK }
        val paintHeader = Paint(paintTitle).apply { textSize = 9f }
        val paintBody = Paint().apply { typeface = Typeface.DEFAULT; textSize = 9f; color = Color.DKGRAY }
        val paintBodyBold = Paint(paintBody).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val paintLine = Paint().apply { strokeWidth = 1f; color = Color.LTGRAY }

        // --- Cabeçalho ---
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

        // --- Tabela de Fases ---
        apontamento.fases.forEach { fase ->
            // Linha divisória
            canvas.drawLine(MARGIN, yPosition - 5, PAGE_WIDTH - MARGIN, yPosition - 5, paintLine)

            // Desenha a foto se existir
            if (fase.caminhoFoto != null) {
                try {
                    val fotoStream = context.contentResolver.openInputStream(Uri.parse(fase.caminhoFoto))
                    val fotoBitmap = BitmapFactory.decodeStream(fotoStream)
                    val scaledFoto = Bitmap.createScaledBitmap(fotoBitmap, 80, 80, true)
                    canvas.drawBitmap(scaledFoto, MARGIN, yPosition, null)
                    fotoStream?.close()
                } catch (e: Exception) {
                    // Se der erro ao carregar a foto, desenha um placeholder
                    canvas.drawRect(MARGIN, yPosition, MARGIN + 80, yPosition + 80, paintLine)
                    canvas.drawText("?", MARGIN + 35, yPosition + 45, paintTitle)
                }
            }

            // Desenha o texto da descrição ao lado da foto
            val textX = if (fase.caminhoFoto != null) MARGIN + 95 else MARGIN
            canvas.drawText("Fase:", textX, yPosition + 10, paintHeader)
            yPosition += 12 // Move para a próxima linha de texto

            // Quebra a descrição em várias linhas se for muito longa
            val descricao = fase.descricao
            var start = 0
            while (start < descricao.length) {
                val end = paintBody.breakText(descricao, start, descricao.length, true, PAGE_WIDTH - textX - MARGIN, null)
                canvas.drawText(descricao, start, start + end, textX, yPosition, paintBody)
                start += end
                yPosition += 12
            }

            yPosition += 10 // Espaço extra após cada fase
            if (yPosition > PAGE_HEIGHT - 50) { // Lógica para nova página (simplificada)
                document.finishPage(page)
                // page = document.startPage(pageInfo) // Inicia nova página se necessário
                // canvas = page.canvas
                yPosition = MARGIN
            }
        }

        document.finishPage(page)
        try {
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }
}