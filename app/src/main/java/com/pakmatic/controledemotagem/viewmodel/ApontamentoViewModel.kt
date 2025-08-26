package com.pakmatic.controledemotagem.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pakmatic.controledemotagem.data.local.Apontamento
import com.pakmatic.controledemotagem.data.local.ApontamentoDao
import com.pakmatic.controledemotagem.data.local.Impedimento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ApontamentoViewModel(private val dao: ApontamentoDao) : ViewModel() {

    private val _nomeResponsavel = MutableStateFlow("")
    val nomeResponsavel = _nomeResponsavel.asStateFlow()
    private val _item = MutableStateFlow("")
    val item = _item.asStateFlow()
    private val _descricaoItem = MutableStateFlow("")
    val descricaoItem = _descricaoItem.asStateFlow()
    private val _descricaoImpedimento = MutableStateFlow("")
    val descricaoImpedimento = _descricaoImpedimento.asStateFlow()
    private val _mostrarDialogImpedimento = MutableStateFlow<Apontamento?>(null)
    val mostrarDialogImpedimento = _mostrarDialogImpedimento.asStateFlow()

    // --- STATEFLOWS PARA CRUD ---
    private val _apontamentoParaEditar = MutableStateFlow<Apontamento?>(null)
    val apontamentoParaEditar = _apontamentoParaEditar.asStateFlow()
    private val _apontamentoParaDeletar = MutableStateFlow<Apontamento?>(null)
    val apontamentoParaDeletar = _apontamentoParaDeletar.asStateFlow()

    val todosApontamentos = dao.buscarTodosComImpedimentos()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun onNomeResponsavelChange(novoNome: String) { _nomeResponsavel.value = novoNome }
    fun onItemChange(novoItem: String) { _item.value = novoItem }
    fun onDescricaoItemChange(novaDescricao: String) { _descricaoItem.value = novaDescricao }
    fun onDescricaoImpedimentoChange(novaDescricao: String) { _descricaoImpedimento.value = novaDescricao }
    fun onAbrirDialogImpedimento(apontamento: Apontamento) { _mostrarDialogImpedimento.value = apontamento }
    fun onFecharDialogImpedimento() { _mostrarDialogImpedimento.value = null }

    // --- FUNÇÕES PARA CRUD ---
    fun onAbrirDialogEdicao(apontamento: Apontamento) {
        _nomeResponsavel.value = apontamento.nomeResponsavel
        _item.value = apontamento.item
        _descricaoItem.value = apontamento.descricaoItem
        _apontamentoParaEditar.value = apontamento
    }
    fun onFecharDialogEdicao() {
        limparCampos()
        _apontamentoParaEditar.value = null
    }

    fun onAbrirDialogDelecao(apontamento: Apontamento) { _apontamentoParaDeletar.value = apontamento }
    fun onFecharDialogDelecao() { _apontamentoParaDeletar.value = null }

    fun salvarEdicao() {
        val apontamentoAntigo = _apontamentoParaEditar.value ?: return
        val apontamentoAtualizado = apontamentoAntigo.copy(
            nomeResponsavel = _nomeResponsavel.value,
            item = _item.value,
            descricaoItem = _descricaoItem.value
        )
        viewModelScope.launch {
            dao.atualizarApontamento(apontamentoAtualizado)
            onFecharDialogEdicao()
        }
    }

    fun confirmarDelecao() {
        val apontamentoAtual = _apontamentoParaDeletar.value ?: return
        viewModelScope.launch {
            dao.deletarApontamento(apontamentoAtual)
            onFecharDialogDelecao()
        }
    }

    fun iniciarNovaMontagem() {
        if (nomeResponsavel.value.isBlank() || item.value.isBlank()) return
        viewModelScope.launch {
            val novoApontamento = Apontamento(
                nomeResponsavel = nomeResponsavel.value, item = item.value,
                descricaoItem = descricaoItem.value, timestampInicio = System.currentTimeMillis(), status = "Em Andamento"
            )
            dao.inserirApontamento(novoApontamento)
            limparCampos()
        }
    }

    // <<< CORREÇÃO APLICADA AQUI >>>
    fun registrarImpedimento() {
        val apontamentoAtual = mostrarDialogImpedimento.value ?: return
        if (descricaoImpedimento.value.isBlank()) return
        viewModelScope.launch {
            val novoImpedimento = Impedimento(
                apontamentoId = apontamentoAtual.id, descricao = descricaoImpedimento.value,
                timestampInicio = System.currentTimeMillis()
            )
            dao.inserirImpedimento(novoImpedimento)

            // Criamos uma cópia com o status atualizado
            val apontamentoAtualizado = apontamentoAtual.copy(status = "Parado")
            dao.atualizarApontamento(apontamentoAtualizado)

            _descricaoImpedimento.value = ""
            onFecharDialogImpedimento()
        }
    }

    // <<< CORREÇÃO APLICADA AQUI >>>
    fun retomarTrabalho(apontamento: Apontamento) {
        viewModelScope.launch {
            val impedimentoAberto = dao.buscarImpedimentoAberto(apontamento.id)
            impedimentoAberto?.let {
                val timestampFinal = System.currentTimeMillis()
                it.timestampFinal = timestampFinal
                it.duracaoSegundos = (timestampFinal - it.timestampInicio) / 1000
                dao.atualizarImpedimento(it)
            }

            // Criamos uma cópia com o status atualizado
            val apontamentoAtualizado = apontamento.copy(status = "Em Andamento")
            dao.atualizarApontamento(apontamentoAtualizado)
        }
    }

    // <<< CORREÇÃO APLICADA AQUI >>>
    fun finalizarMontagem(apontamento: Apontamento) {
        viewModelScope.launch {
            // Criamos uma cópia com o status e tempo final atualizados
            val apontamentoAtualizado = apontamento.copy(
                timestampFinal = System.currentTimeMillis(),
                status = "Finalizado"
            )
            dao.atualizarApontamento(apontamentoAtualizado)
        }
    }

    private fun limparCampos() {
        _nomeResponsavel.value = ""
        _item.value = ""
        _descricaoItem.value = ""
    }
    fun exportarRelatorioTxt(context: Context, uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val dados = todosApontamentos.value
            val textoFormatado = com.pakmatic.controledemotagem.util.TxtExporter.formatarParaTxt(dados)
            context.contentResolver.openOutputStream(uri)?.use { it.write(textoFormatado.toByteArray()) }
        }
    }
    fun exportarRelatorioPdf(context: Context, uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val dados = todosApontamentos.value
            context.contentResolver.openOutputStream(uri)?.use { com.pakmatic.controledemotagem.util.PdfExporter.gerarPdf(context, it, dados) }
        }
    }
}

class ApontamentoViewModelFactory(private val dao: ApontamentoDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ApontamentoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ApontamentoViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}