package com.pakmatic.controledemotagem.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pakmatic.controledemotagem.data.local.*
import com.pakmatic.controledemotagem.util.PdfExporterDetalhado
import com.pakmatic.controledemotagem.util.TxtExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ApontamentoViewModel(private val dao: ApontamentoDao) : ViewModel() {

    // (O início do arquivo permanece o mesmo)
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
    private val _apontamentoParaEditar = MutableStateFlow<Apontamento?>(null)
    val apontamentoParaEditar = _apontamentoParaEditar.asStateFlow()
    private val _apontamentoParaDeletar = MutableStateFlow<Apontamento?>(null)
    val apontamentoParaDeletar = _apontamentoParaDeletar.asStateFlow()
    private val _descricaoFase = MutableStateFlow("")
    val descricaoFase = _descricaoFase.asStateFlow()
    private val _apontamentoParaNovaFase = MutableStateFlow<Apontamento?>(null)
    val apontamentoParaNovaFase = _apontamentoParaNovaFase.asStateFlow()
    val todosApontamentos = dao.buscarTodosCompletos()
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
    fun onAbrirDialogEdicao(apontamento: Apontamento) {
        _nomeResponsavel.value = apontamento.nomeResponsavel
        _item.value = apontamento.item
        _descricaoItem.value = apontamento.descricaoItem
        _apontamentoParaEditar.value = apontamento
    }
    fun onFecharDialogEdicao() { limparCampos(); _apontamentoParaEditar.value = null }
    fun onAbrirDialogDelecao(apontamento: Apontamento) { _apontamentoParaDeletar.value = apontamento }
    fun onFecharDialogDelecao() { _apontamentoParaDeletar.value = null }
    fun onDescricaoFaseChange(novaDescricao: String) { _descricaoFase.value = novaDescricao }
    fun onAbrirDialogNovaFase(apontamento: Apontamento) { _apontamentoParaNovaFase.value = apontamento }
    fun onFecharDialogNovaFase() { _descricaoFase.value = ""; _apontamentoParaNovaFase.value = null }
    fun iniciarNovaFase() {
        val apontamentoAtual = _apontamentoParaNovaFase.value ?: return
        if (_descricaoFase.value.isBlank()) return
        viewModelScope.launch {
            val novaFase = Fase(apontamentoId = apontamentoAtual.id, descricao = _descricaoFase.value, timestampInicio = System.currentTimeMillis())
            dao.inserirFase(novaFase)
            onFecharDialogNovaFase()
        }
    }
    fun finalizarFase(fase: Fase) {
        viewModelScope.launch {
            val timestampFinal = System.currentTimeMillis()
            val faseAtualizada = fase.copy(timestampFinal = timestampFinal, duracaoSegundos = (timestampFinal - fase.timestampInicio) / 1000)
            dao.atualizarFase(faseAtualizada)
        }
    }
    fun salvarCaminhoFoto(fase: Fase, fotoUri: Uri) {
        viewModelScope.launch {
            val faseAtualizada = fase.copy(caminhoFoto = fotoUri.toString())
            dao.atualizarFase(faseAtualizada)
        }
    }
    fun salvarEdicao() {
        val apontamentoAntigo = _apontamentoParaEditar.value ?: return
        val apontamentoAtualizado = apontamentoAntigo.copy(nomeResponsavel = _nomeResponsavel.value, item = _item.value, descricaoItem = _descricaoItem.value)
        viewModelScope.launch { dao.atualizarApontamento(apontamentoAtualizado); onFecharDialogEdicao() }
    }
    fun confirmarDelecao() {
        val apontamentoAtual = _apontamentoParaDeletar.value ?: return
        viewModelScope.launch { dao.deletarApontamento(apontamentoAtual); onFecharDialogDelecao() }
    }
    fun iniciarNovaMontagem() {
        if (nomeResponsavel.value.isBlank() || item.value.isBlank()) return
        viewModelScope.launch {
            val novoApontamento = Apontamento(nomeResponsavel = nomeResponsavel.value, item = item.value, descricaoItem = descricaoItem.value, timestampInicio = System.currentTimeMillis(), status = "Em Andamento")
            dao.inserirApontamento(novoApontamento); limparCampos()
        }
    }
    fun registrarImpedimento() {
        val apontamentoAtual = mostrarDialogImpedimento.value ?: return
        if (descricaoImpedimento.value.isBlank()) return
        viewModelScope.launch {
            val novoImpedimento = Impedimento(apontamentoId = apontamentoAtual.id, descricao = descricaoImpedimento.value, timestampInicio = System.currentTimeMillis())
            dao.inserirImpedimento(novoImpedimento)
            val apontamentoAtualizado = apontamentoAtual.copy(status = "Parado")
            dao.atualizarApontamento(apontamentoAtualizado)
            _descricaoImpedimento.value = ""; onFecharDialogImpedimento()
        }
    }
    fun retomarTrabalho(apontamento: Apontamento) {
        viewModelScope.launch {
            val impedimentoAberto = dao.buscarImpedimentoAberto(apontamento.id)
            impedimentoAberto?.let {
                val timestampFinal = System.currentTimeMillis()
                val impedimentoAtualizado = it.copy(timestampFinal = timestampFinal, duracaoSegundos = (timestampFinal - it.timestampInicio) / 1000)
                dao.atualizarImpedimento(impedimentoAtualizado)
            }
            val apontamentoAtualizado = apontamento.copy(status = "Em Andamento")
            dao.atualizarApontamento(apontamentoAtualizado)
        }
    }
    fun finalizarMontagem(apontamento: Apontamento) {
        viewModelScope.launch {
            val apontamentoAtualizado = apontamento.copy(timestampFinal = System.currentTimeMillis(), status = "Finalizado")
            dao.atualizarApontamento(apontamentoAtualizado)
        }
    }
    private fun limparCampos() { _nomeResponsavel.value = ""; _item.value = ""; _descricaoItem.value = "" }

    fun exportarRelatorioTxt(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val dados = todosApontamentos.value
            val textoFormatado = TxtExporter.formatarParaTxt(dados)
            context.contentResolver.openOutputStream(uri)?.use { it.write(textoFormatado.toByteArray()) }
        }
    }
    fun exportarRelatorioPdf(context: Context, uri: Uri) {
        // Esta função fica para o PDF Simples, que ainda precisa ser implementado ou adaptado.
    }

    // <<< NOVA FUNÇÃO ADICIONADA AQUI >>>
    fun exportarRelatorioDetalhadoPdf(context: Context, uri: Uri, apontamento: ApontamentoCompleto) {
        viewModelScope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                PdfExporterDetalhado.gerarPdf(context, outputStream, apontamento)
            }
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