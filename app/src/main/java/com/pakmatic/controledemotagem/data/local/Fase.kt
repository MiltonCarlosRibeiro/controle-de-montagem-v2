package com.pakmatic.controledemotagem.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "fases",
    foreignKeys = [
        ForeignKey(
            entity = Apontamento::class,
            parentColumns = ["id"],
            childColumns = ["apontamentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Fase(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val apontamentoId: Int, // Chave para ligar à montagem principal
    val descricao: String,
    val timestampInicio: Long,
    var timestampFinal: Long? = null,
    var duracaoSegundos: Long = 0,
    var caminhoFoto: String? = null // Caminho para a foto salva no dispositivo
)