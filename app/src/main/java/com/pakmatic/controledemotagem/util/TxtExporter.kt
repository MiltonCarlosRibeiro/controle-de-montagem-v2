package com.pakmatic.controledemotagem.util

import com.pakmatic.controledemotagem.data.local.ApontamentoComImpedimentos
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object TxtExporter {

    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null) return "".padEnd(17)
        return SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDuration(totalSeconds: Long): String {
        val hours = TimeUnit.SECONDS.toHours(totalSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatarParaTxt(apontamentos: List<ApontamentoComImpedimentos>): String {
        val builder = StringBuilder()
        val separador = "-".repeat(120) + "\n"

        builder.append("RELATÓRIO DE MONTAGEM - PAKMATIC\n")
        builder.append("Máquina em montagem: [Nome da Máquina]\n")
        builder.append("NS: [Número de Série] | PP: [Projeto]\n")
        builder.append(separador)

        builder.append(
            "RESPONSÁVEL".padEnd(20) +
                    "ITEM".padEnd(15) +
                    "INÍCIO".padEnd(20) +
                    "FINAL".padEnd(20) +
                    "T. PARADO".padEnd(15) +
                    "STATUS".padEnd(15) + "\n"
        )
        builder.append(separador)

        if (apontamentos.isEmpty()) {
            builder.append("Nenhum apontamento para exibir.\n")
        } else {
            apontamentos.forEach { item ->
                val apontamento = item.apontamento
                builder.append(
                    apontamento.nomeResponsavel.take(19).padEnd(20) +
                            apontamento.item.take(14).padEnd(15) +
                            formatTimestamp(apontamento.timestampInicio).padEnd(20) +
                            formatTimestamp(apontamento.timestampFinal).padEnd(20) +
                            formatDuration(item.tempoTotalParadoSegundos).padEnd(15) +
                            apontamento.status.take(14).padEnd(15) + "\n"
                )
                if (item.impedimentos.isNotEmpty()) {
                    builder.append("  ↳ Impedimentos:\n")
                    item.impedimentos.forEach { impedimento ->
                        builder.append(
                            "    - Início: ${formatTimestamp(impedimento.timestampInicio)} | " +
                                    "Duração: ${formatDuration(impedimento.duracaoSegundos)} | " +
                                    "Motivo: ${impedimento.descricao}\n"
                        )
                    }
                }
                builder.append("\n")
            }
        }

        builder.append(separador)
        builder.append("Relatório gerado em: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")

        return builder.toString()
    }
}