package com.pakmatic.controledemotagem.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotEmpty() } ?: listOf()
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }
}

@Entity(
    tableName = "fases",
    foreignKeys = [
        ForeignKey(
            entity = Apontamento::class,
            parentColumns = ["id"],
            childColumns = ["apontamentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["apontamentoId"])]
)
@TypeConverters(Converters::class)
data class Fase(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val apontamentoId: Int,
    val descricao: String,
    val timestampInicio: Long,
    var timestampFinal: Long? = null,
    var duracaoSegundos: Long = 0,
    var caminhosFotos: List<String> = listOf()
)