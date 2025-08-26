package com.pakmatic.controledemotagem.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ApontamentoDao {

    // --- Métodos para Apontamentos ---
    @Insert
    suspend fun inserirApontamento(apontamento: Apontamento)

    @Update
    suspend fun atualizarApontamento(apontamento: Apontamento)

    @Delete
    suspend fun deletarApontamento(apontamento: Apontamento)

    // <<< QUERY ATUALIZADA para usar a nova classe de relação >>>
    @Transaction
    @Query("SELECT * FROM apontamentos ORDER BY timestampInicio DESC")
    fun buscarTodosCompletos(): Flow<List<ApontamentoCompleto>>

    // --- Métodos para Impedimentos ---
    @Insert
    suspend fun inserirImpedimento(impedimento: Impedimento)

    @Update
    suspend fun atualizarImpedimento(impedimento: Impedimento)

    @Query("SELECT * FROM impedimentos WHERE apontamentoId = :apontamentoId AND timestampFinal IS NULL ORDER BY timestampInicio DESC LIMIT 1")
    suspend fun buscarImpedimentoAberto(apontamentoId: Int): Impedimento?

    // --- NOVOS MÉTODOS PARA FASES ---
    @Insert
    suspend fun inserirFase(fase: Fase)

    @Update
    suspend fun atualizarFase(fase: Fase)

    @Query("SELECT * FROM fases WHERE apontamentoId = :apontamentoId AND timestampFinal IS NULL ORDER BY timestampInicio DESC LIMIT 1")
    suspend fun buscarFaseAberta(apontamentoId: Int): Fase?
}