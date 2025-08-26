package com.pakmatic.controledemotagem.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ApontamentoViewModel(private val dao: ApontamentoDao) : ViewModel() {

    // (O início do ViewModel permanece o mesmo, sem alterações)
    private val _nomeResponsavel = MutableStateFlow("")
    val nomeResponsavel = _nomeResponsavel.asStateFlow()
    private val _item = MutableStateFlow("")
    val item = _item.asStateFlow()
    private val _descricaoItem = MutableStateFlow("")
    val descricaoItem = _descricaoItem.asStateFlow()
    private val _dataInicioEdit = MutableStateFlow("")
    val dataInicioEdit = _dataInicioEdit.asStateFlow()
    private val _horaInicioEdit = MutableStateFlow("")
    val horaInicioEdit = _horaInicioEdit.asStateFlow()
    private val _dataFinalEdit = MutableStateFlow("")
    val dataFinalEdit = _dataFinalEdit.asStateFlow()
    private val _horaFinalEdit = MutableStateFlow("")
    val horaFinalEdit = _horaFinalEdit.asStateFlow()
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
    val todosApontamentos = dao.buscarTodosCompletos().stateIn(scope = viewModelScope, started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())
    fun onNomeResponsavelChange(novoNome: String) { _nomeResponsavel.value = novoNome }
    fun onItemChange(novoItem: String) { _item.value = novoItem }
    fun onDescricaoItemChange(novaDescricao: String) { _descricaoItem.value = novaDescricao }
    fun onDescricaoImpedimentoChange(novaDescricao: String) { _descricaoImpedimento.value = novaDescricao }
    fun onDescricaoFaseChange(novaDescricao: String) { _descricaoFase.value = novaDescricao }
    fun onDataInicioChange(novaData: String) { _dataInicioEdit.value = novaData }
    fun onHoraInicioChange(novaHora: String) { _horaInicioEdit.value = novaHora }
    fun onDataFinalChange(novaData: String) { _dataFinalEdit.value = novaData }
    fun onHoraFinalChange(novaHora: String) { _horaFinalEdit.value = novaHora }
    fun onAbrirDialogImpedimento(apontamento: Apontamento) { _mostrarDialogImpedimento.value = apontamento }
    fun onFecharDialogImpedimento() { _mostrarDialogImpedimento.value = null }
    fun onAbrirDialogDelecao(apontamento: Apontamento) { _apontamentoParaDeletar.value = apontamento }
    fun onFecharDialogDelecao() { _apontamentoParaDeletar.value = null }
    fun onAbrirDialogNovaFase(apontamento: Apontamento) { _apontamentoParaNovaFase.value = apontamento }
    fun onFecharDialogNovaFase() { _descricaoFase.value = ""; _apontamentoParaNovaFase.value = null }
    fun onAbrirDialogEdicao(apontamento: Apontamento) {
        _nomeResponsavel.value = apontamento.nomeResponsavel
        _item.value = apontamento.item
        _descricaoItem.value = apontamento.descricaoItem
        val sdfData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        _dataInicioEdit.value = sdfData.format(Date(apontamento.timestampInicio))
        _horaInicioEdit.value = sdfHora.format(Date(apontamento.timestampInicio))
        apontamento.timestampFinal?.let { _dataFinalEdit.value = sdfData.format(Date(it)); _horaFinalEdit.value = sdfHora.format(Date(it)) } ?: run { _dataFinalEdit.value = ""; _horaFinalEdit.value = "" }
        _apontamentoParaEditar.value = apontamento
    }
    fun onFecharDialogEdicao() { limparCampos(); _apontamentoParaEditar.value = null }

    fun salvarEdicao() {
        val apontamentoAntigo = _apontamentoParaEditar.value ?: return
        try {
            val sdfCompleto = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val novoTimestampInicio = sdfCompleto.parse("${_dataInicioEdit.value} ${_horaInicioEdit.value}")?.time ?: apontamentoAntigo.timestampInicio
            val novoTimestampFinal = if (_dataFinalEdit.value.isNotBlank() && _horaFinalEdit.value.isNotBlank()) { sdfCompleto.parse("${_dataFinalEdit.value} ${_horaFinalEdit.value}")?.time } else { apontamentoAntigo.timestampFinal }
            val apontamentoAtualizado = apontamentoAntigo.copy(nomeResponsavel = _nomeResponsavel.value, item = _item.value, descricaoItem = _descricaoItem.value, timestampInicio = novoTimestampInicio, timestampFinal = novoTimestampFinal)
            viewModelScope.launch { dao.atualizarApontamento(apontamentoAtualizado); onFecharDialogEdicao() }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun reabrirMontagem(apontamento: Apontamento) {
        viewModelScope.launch {
            val apontamentoReaberto = apontamento.copy(status = "Em Andamento", timestampFinal = null)
            dao.atualizarApontamento(apontamentoReaberto)
        }
    }

    fun adicionarFotoAFase(fase: Fase, fotoUri: Uri) {
        viewModelScope.launch {
            val novaListaDeFotos = fase.caminhosFotos.toMutableList().apply { add(fotoUri.toString()) }
            val faseAtualizada = fase.copy(caminhosFotos = novaListaDeFotos)
            dao.atualizarFase(faseAtualizada)
        }
    }

    fun substituirFotoDaFase(fase: Fase, fotoAntigaUri: String, novaFotoUri: Uri) {
        viewModelScope.launch {
            val novaListaDeFotos = fase.caminhosFotos.toMutableList().map { if (it == fotoAntigaUri) novaFotoUri.toString() else it }
            val faseAtualizada = fase.copy(caminhosFotos = novaListaDeFotos)
            dao.atualizarFase(faseAtualizada)
        }
    }

    fun girarFotoDaFase(context: Context, fase: Fase, fotoUriString: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val fotoUri = Uri.parse(fotoUriString)
                    context.contentResolver.openInputStream(fotoUri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val matrix = Matrix().apply { postRotate(90f) }
                        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                        context.contentResolver.openOutputStream(fotoUri)?.use { outputStream ->
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                        }
                    }
                    val faseAtualizada = fase.copy(caminhosFotos = fase.caminhosFotos.map { it })
                    withContext(Dispatchers.Main) {
                        dao.atualizarFase(faseAtualizada)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    fun confirmarDelecao() { val apontamentoAtual = _apontamentoParaDeletar.value ?: return; viewModelScope.launch { dao.deletarApontamento(apontamentoAtual); onFecharDialogDelecao() } }
    fun iniciarNovaMontagem() { if (nomeResponsavel.value.isBlank() || item.value.isBlank()) return; viewModelScope.launch { val novoApontamento = Apontamento(nomeResponsavel = nomeResponsavel.value, item = item.value, descricaoItem = descricaoItem.value, timestampInicio = System.currentTimeMillis(), status = "Em Andamento"); dao.inserirApontamento(novoApontamento); limparCampos() } }
    fun registrarImpedimento() { val apontamentoAtual = mostrarDialogImpedimento.value ?: return; if (descricaoImpedimento.value.isBlank()) return; viewModelScope.launch { val novoImpedimento = Impedimento(apontamentoId = apontamentoAtual.id, descricao = descricaoImpedimento.value, timestampInicio = System.currentTimeMillis()); dao.inserirImpedimento(novoImpedimento); val apontamentoAtualizado = apontamentoAtual.copy(status = "Parado"); dao.atualizarApontamento(apontamentoAtualizado); _descricaoImpedimento.value = ""; onFecharDialogImpedimento() } }
    fun retomarTrabalho(apontamento: Apontamento) { viewModelScope.launch { val impedimentoAberto = dao.buscarImpedimentoAberto(apontamento.id); impedimentoAberto?.let { val timestampFinal = System.currentTimeMillis(); val impedimentoAtualizado = it.copy(timestampFinal = timestampFinal, duracaoSegundos = (timestampFinal - it.timestampInicio) / 1000); dao.atualizarImpedimento(impedimentoAtualizado) }; val apontamentoAtualizado = apontamento.copy(status = "Em Andamento"); dao.atualizarApontamento(apontamentoAtualizado) } }
    fun finalizarMontagem(apontamento: Apontamento) { viewModelScope.launch { val apontamentoAtualizado = apontamento.copy(timestampFinal = System.currentTimeMillis(), status = "Finalizado"); dao.atualizarApontamento(apontamentoAtualizado) } }
    fun iniciarNovaFase() { val apontamentoAtual = _apontamentoParaNovaFase.value ?: return; if (_descricaoFase.value.isBlank()) return; viewModelScope.launch { val novaFase = Fase(apontamentoId = apontamentoAtual.id, descricao = _descricaoFase.value, timestampInicio = System.currentTimeMillis()); dao.inserirFase(novaFase); onFecharDialogNovaFase() } }
    fun finalizarFase(fase: Fase) { viewModelScope.launch { val timestampFinal = System.currentTimeMillis(); val faseAtualizada = fase.copy(timestampFinal = timestampFinal, duracaoSegundos = (timestampFinal - fase.timestampInicio) / 1000); dao.atualizarFase(faseAtualizada) } }
    private fun limparCampos() { _nomeResponsavel.value = ""; _item.value = ""; _descricaoItem.value = "" }
    fun exportarRelatorioTxt(context: Context, uri: Uri) { viewModelScope.launch(Dispatchers.IO) { val dados = todosApontamentos.value; val textoFormatado = TxtExporter.formatarParaTxt(dados); context.contentResolver.openOutputStream(uri)?.use { it.write(textoFormatado.toByteArray()) } } }

    // <<< FUNÇÃO CORRIGIDA >>>
    fun exportarRelatorioDetalhadoPdf(context: Context, uri: Uri, apontamentos: List<ApontamentoCompleto>) {
        viewModelScope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                PdfExporterDetalhado.gerarPdf(context, outputStream, apontamentos)
            }
        }
    }
}

class ApontamentoViewModelFactory(private val dao: ApontamentoDao) : ViewModelProvider.Factory { override fun <T : ViewModel> create(modelClass: Class<T>): T { if (modelClass.isAssignableFrom(ApontamentoViewModel::class.java)) { @Suppress("UNCHECKED_CAST") return ApontamentoViewModel(dao) as T }; throw IllegalArgumentException("Unknown ViewModel class") } }