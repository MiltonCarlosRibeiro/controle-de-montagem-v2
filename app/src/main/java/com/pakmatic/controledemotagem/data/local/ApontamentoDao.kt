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

    @Transaction
    @Query("SELECT * FROM apontamentos ORDER BY timestampInicio DESC")
    fun buscarTodosComImpedimentos(): Flow<List<ApontamentoComImpedimentos>>

    // <<< CORREÇÃO AQUI >>>
    // Trocamos a query manual pela anotação @Delete, que é mais segura.
    // Ela deleta o item com base na chave primária (id) do objeto passado.
    @Delete
    suspend fun deletarApontamento(apontamento: Apontamento)


    // --- Métodos para Impedimentos ---
    @Insert
    suspend fun inserirImpedimento(impedimento: Impedimento)

    @Update
    suspend fun atualizarImpedimento(impedimento: Impedimento)

    @Query("SELECT * FROM impedimentos WHERE apontamentoId = :apontamentoId AND timestampFinal IS NULL ORDER BY timestampInicio DESC LIMIT 1")
    suspend fun buscarImpedimentoAberto(apontamentoId: Int): Impedimento?
}