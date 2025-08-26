package com.pakmatic.controledemotagem.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apontamentos")
data class Apontamento(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nomeResponsavel: String,
    val item: String,
    val descricaoItem: String,
    val timestampInicio: Long,
    var timestampFinal: Long? = null, // Pode ser nulo até a finalização
    var status: String // "Em Andamento", "Parado", "Finalizado"
)