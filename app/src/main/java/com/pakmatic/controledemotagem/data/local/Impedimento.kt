package com.pakmatic.controledemotagem.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

// Nova tabela para registrar cada impedimento individualmente
@Entity(
    tableName = "impedimentos",
    foreignKeys = [
        ForeignKey(
            entity = Apontamento::class,
            parentColumns = ["id"],
            childColumns = ["apontamentoId"],
            onDelete = ForeignKey.CASCADE // Se o apontamento for deletado, seus impedimentos também serão.
        )
    ]
)
data class Impedimento(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val apontamentoId: Int, // Chave estrangeira para ligar ao Apontamento
    val descricao: String,
    val timestampInicio: Long,
    var timestampFinal: Long? = null,
    var duracaoSegundos: Long = 0
)

// Classe de Relação para buscar um Apontamento com sua lista de Impedimentos
data class ApontamentoComImpedimentos(
    @Embedded val apontamento: Apontamento,
    @Relation(
        parentColumn = "id",
        entityColumn = "apontamentoId"
    )
    val impedimentos: List<Impedimento>
) {
    // Lógica para calcular o tempo total parado somando a duração de todos os impedimentos
    val tempoTotalParadoSegundos: Long
        get() = impedimentos.sumOf { it.duracaoSegundos }
}