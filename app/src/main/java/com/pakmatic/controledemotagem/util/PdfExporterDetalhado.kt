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
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = MARGIN

            val paintTitle = Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 16f; color = Color.BLACK }
            val paintHeader = Paint(paintTitle).apply { textSize = 9f }
            val paintBody = Paint().apply { typeface = Typeface.DEFAULT; textSize = 9f; color = Color.DKGRAY }
            val paintLine = Paint().apply { strokeWidth = 1f; color = Color.LTGRAY }

            // --- Cabeçalho ---
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

            // --- Tabela de Fases ---
            apontamento.fases.forEach { fase ->
                // Checa se precisa de uma nova página ANTES de desenhar o item
                if (yPosition > PAGE_HEIGHT - 150) { // Margem de segurança maior para fotos
                    document.finishPage(page)
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = MARGIN
                }

                canvas.drawLine(MARGIN, yPosition - 5, PAGE_WIDTH - MARGIN, yPosition - 5, paintLine)

                // --- Lógica de Quebra de Linha para Descrição ---
                val textYStart = yPosition + 10
                var textY = textYStart
                val descricao = fase.descricao
                var start = 0
                while (start < descricao.length) {
                    val end = paintBody.breakText(descricao, start, descricao.length, true, PAGE_WIDTH - MARGIN * 2, null)
                    canvas.drawText(descricao, start, start + end, MARGIN, textY, paintBody)
                    start += end
                    textY += 12
                }
                yPosition = textY // Atualiza a posição Y após o texto

                // <<< LÓGICA ATUALIZADA PARA DESENHAR TODAS AS FOTOS >>>
                if (fase.caminhosFotos.isNotEmpty()) {
                    val fotoSize = 255f // 9cm em pontos
                    val fotoSpacing = 8f
                    var currentX = MARGIN
                    yPosition += 5 // Espaço entre o texto e as fotos

                    fase.caminhosFotos.forEach { fotoUriString ->
                        // Se a próxima foto não couber na linha, quebra a linha
                        if (currentX + fotoSize > PAGE_WIDTH - MARGIN) {
                            currentX = MARGIN
                            yPosition += fotoSize + fotoSpacing
                        }

                        // Checa se a nova linha de fotos cabe na página
                        if (yPosition > PAGE_HEIGHT - 80) {
                            document.finishPage(page)
                            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create()
                            page = document.startPage(pageInfo)
                            canvas = page.canvas
                            yPosition = MARGIN
                            currentX = MARGIN
                        }

                        try {
                            val fotoStream = context.contentResolver.openInputStream(Uri.parse(fotoUriString))
                            val fotoBitmap = BitmapFactory.decodeStream(fotoStream)
                            val scaledFoto = Bitmap.createScaledBitmap(fotoBitmap, fotoSize.toInt(), fotoSize.toInt(), true)
                            canvas.drawBitmap(scaledFoto, currentX, yPosition, null)
                            fotoStream?.close()
                        } catch (e: Exception) {
                            // Desenha um placeholder se a foto falhar
                            canvas.drawRect(currentX, yPosition, currentX + fotoSize, yPosition + fotoSize, paintLine)
                        }
                        currentX += fotoSize + fotoSpacing
                    }
                    yPosition += fotoSize + fotoSpacing + 15 // Atualiza a posição Y após a última linha de fotos
                }
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