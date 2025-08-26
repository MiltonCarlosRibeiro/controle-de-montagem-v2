package com.pakmatic.controledemotagem.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apontamentos")
data class Apontamento(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var nomeResponsavel: String,
    var item: String,
    var descricaoItem: String,
    var timestampInicio: Long,
    var timestampFinal: Long? = null,
    var status: String
)