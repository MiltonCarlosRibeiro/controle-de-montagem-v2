package com.pakmatic.controledemotagem.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

// (A classe Impedimento continua a mesma)
@Entity(
    tableName = "impedimentos",
    foreignKeys = [
        ForeignKey(
            entity = Apontamento::class,
            parentColumns = ["id"],
            childColumns = ["apontamentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Impedimento(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val apontamentoId: Int,
    val descricao: String,
    val timestampInicio: Long,
    var timestampFinal: Long? = null,
    var duracaoSegundos: Long = 0
)

// <<< CLASSE ATUALIZADA/RENOMEADA >>>
// Renomeamos para ApontamentoCompleto para refletir que agora ela busca tudo
data class ApontamentoCompleto(
    @Embedded val apontamento: Apontamento,
    @Relation(
        parentColumn = "id",
        entityColumn = "apontamentoId"
    )
    val impedimentos: List<Impedimento>,

    // <<< NOVA RELAÇÃO ADICIONADA >>>
    @Relation(
        parentColumn = "id",
        entityColumn = "apontamentoId"
    )
    val fases: List<Fase>
) {
    val tempoTotalParadoSegundos: Long
        get() = impedimentos.sumOf { it.duracaoSegundos }
}