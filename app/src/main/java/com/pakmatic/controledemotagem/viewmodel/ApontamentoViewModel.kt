package com.pakmatic.controledemotagem.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pakmatic.controledemotagem.data.local.*
import com.pakmatic.controledemotagem.util.PdfExporterDetalhado
import com.pakmatic.controledemotagem.util.TxtExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ApontamentoViewModel(private val dao: ApontamentoDao, application: Application) : AndroidViewModel(application) {

    // Tag única para todos os logs desta classe
    private val TAG = "ApontamentoViewModelLogs"

    private val appContext = application.applicationContext

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
    private val _faseParaEditar = MutableStateFlow<Fase?>(null)
    val faseParaEditar = _faseParaEditar.asStateFlow()
    private val _isCronometroPausado = MutableStateFlow(false)
    val isCronometroPausado: StateFlow<Boolean> = _isCronometroPausado.asStateFlow()

    val todosApontamentos = dao.buscarTodosCompletos().stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())

    // Funções de controle de estado e CRUD
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
    fun onAbrirDialogEdicaoFase(fase: Fase) {
        _descricaoFase.value = fase.descricao
        _faseParaEditar.value = fase
    }
    fun onFecharDialogEdicaoFase() {
        _descricaoFase.value = ""
        _faseParaEditar.value = null
    }
    fun salvarEdicaoFase() {
        val faseAntiga = _faseParaEditar.value ?: return
        val faseAtualizada = faseAntiga.copy(descricao = _descricaoFase.value)
        viewModelScope.launch {
            dao.atualizarFase(faseAtualizada)
            onFecharDialogEdicaoFase()
        }
    }
    fun deletarFase(fase: Fase) {
        viewModelScope.launch {
            dao.deletarFase(fase)
        }
    }
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
            val apontamentoReaberto = apontamento.copy(status = "Parado", timestampFinal = null)
            dao.atualizarApontamento(apontamentoReaberto)
            _isCronometroPausado.value = true
        }
    }
    fun toggleCronometro() {
        _isCronometroPausado.value = !_isCronometroPausado.value
        if (!_isCronometroPausado.value) {
            viewModelScope.launch(Dispatchers.IO) {
                todosApontamentos.value.firstOrNull { it.apontamento.status == "Parado" }?.let { completo ->
                    val apontamento = completo.apontamento
                    dao.atualizarApontamento(apontamento.copy(status = "Em Andamento"))
                }
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                todosApontamentos.value.firstOrNull { it.apontamento.status == "Em Andamento" }?.let { completo ->
                    val apontamento = completo.apontamento
                    dao.atualizarApontamento(apontamento.copy(status = "Parado"))
                }
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap? {
        Log.d(TAG, "rotateBitmap: Rotacionando bitmap em $degrees graus.")
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

//    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri? {
//        Log.d(TAG, "saveBitmapToFile: Iniciando salvamento de bitmap.")
//        // --- CORREÇÃO APLICADA AQUI ---
//        // Alterado de context.cacheDir para context.filesDir para armazenamento persistente.
//        val outputDir = File(context.filesDir, "images")
//        if (!outputDir.exists()) {
//            outputDir.mkdirs()
//            Log.d(TAG, "saveBitmapToFile: Diretório 'images' criado em: ${outputDir.absolutePath}")
//        } else {
//            Log.d(TAG, "saveBitmapToFile: Usando diretório existente: ${outputDir.absolutePath}")
//        }
//
//        val photoFile = File.createTempFile(
//            "img_${System.currentTimeMillis()}_",
//            ".jpg",
//            outputDir
//        )
//        Log.d(TAG, "saveBitmapToFile: Arquivo temporário criado em: ${photoFile.absolutePath}")
//
//        return try {
//            FileOutputStream(photoFile).use { out ->
//                // Redimensionando e comprimindo a imagem
//                val targetWidth = 480
//                val targetHeight = 854
//                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
//                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
//                Log.d(TAG, "saveBitmapToFile: Bitmap comprimido e salvo com sucesso.")
//            }
//            val fileUri = FileProvider.getUriForFile(
//                context,
//                "${context.packageName}.provider",
//                photoFile
//            )
//            Log.d(TAG, "saveBitmapToFile: Uri gerada pelo FileProvider: $fileUri")
//            fileUri
//        } catch (e: Exception) {
//            Log.e(TAG, "saveBitmapToFile: Erro ao salvar bitmap: ${e.message}", e)
//            null
//        }
//    }

    // ADICIONE ESTA NOVA FUNÇÃO PRIVADA
    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, albumName: String): Uri? {
        val displayName = "IMG_${System.currentTimeMillis()}.jpg"
        val mimeType = "image/jpeg"
        val compressFormat = Bitmap.CompressFormat.JPEG

        // Lógica para Android 10 (API 29) e superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$albumName")
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                try {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        if (!bitmap.compress(compressFormat, 95, outputStream)) { return null }
                    }
                    return it
                } catch (e: Exception) {
                    resolver.delete(it, null, null)
                    e.printStackTrace()
                    return null
                }
            }
            return null

        } else {
            // Lógica para Android 9 (API 28) e inferior
            // IMPORTANTE: Este trecho assume que a permissão WRITE_EXTERNAL_STORAGE já foi concedida!
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val albumDir = File(picturesDir, albumName)
            if (!albumDir.exists()) {
                albumDir.mkdirs()
            }
            val imageFile = File(albumDir, displayName)

            try {
                FileOutputStream(imageFile).use { outputStream ->
                    if (!bitmap.compress(compressFormat, 95, outputStream)) { return null }
                }
                MediaScannerConnection.scanFile(context, arrayOf(imageFile.absolutePath), arrayOf(mimeType), null)
                return Uri.fromFile(imageFile)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }




    private suspend fun processAndSavePhoto(context: Context, originalUri: Uri): Uri? = withContext(Dispatchers.IO) {
        Log.d(TAG, "processAndSavePhoto: Iniciando processamento para a Uri original: $originalUri")
        try {
            context.contentResolver.openInputStream(originalUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                Log.d(TAG, "processAndSavePhoto: Bitmap decodificado da Uri original. Dimensões: ${bitmap.width}x${bitmap.height}")
                val rotatedBitmap = if (bitmap.width > bitmap.height) {
                    Log.d(TAG, "processAndSavePhoto: A imagem está em paisagem, rotacionando...")
                    rotateBitmap(bitmap, 90f)
                } else {
                    Log.d(TAG, "processAndSavePhoto: A imagem está em retrato, não precisa rotacionar.")
                    bitmap
                }
                rotatedBitmap?.let {
                    saveBitmapToGallery(context, it, "ControleDeMontagem")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "processAndSavePhoto: Erro ao processar foto: ${e.message}", e)
            null
        }
    }
    fun adicionarFotoAFase(fase: Fase, fotoUri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        Log.d(TAG, "adicionarFotoAFase: Tentando adicionar foto à fase ID: ${fase.id}")
        val savedUri = processAndSavePhoto(appContext, fotoUri)
        if (savedUri != null) {
            Log.d(TAG, "adicionarFotoAFase: Foto salva com sucesso. Nova Uri: $savedUri")
            val novaListaDeFotos = fase.caminhosFotos.toMutableList().apply { add(savedUri.toString()) }
            dao.atualizarFase(fase.copy(caminhosFotos = novaListaDeFotos))
            Log.d(TAG, "adicionarFotoAFase: Fase ID: ${fase.id} atualizada no banco de dados com a nova foto.")
        } else {
            Log.e(TAG, "adicionarFotoAFase: Falha ao salvar a foto. A fase não foi atualizada.")
        }
    }
    fun substituirFotoDaFase(fase: Fase, oldUriString: String, newUri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        Log.d(TAG, "substituirFotoDaFase: Tentando substituir foto na fase ID: ${fase.id}. Uri antiga: $oldUriString")
        val savedUri = processAndSavePhoto(appContext, newUri)
        if (savedUri != null) {
            Log.d(TAG, "substituirFotoDaFase: Nova foto salva com sucesso. Nova Uri: $savedUri")
            val novaListaDeFotos = fase.caminhosFotos.toMutableList().apply {
                val index = indexOf(oldUriString)
                if (index != -1) {
                    set(index, savedUri.toString())
                    Log.d(TAG, "substituirFotoDaFase: Uri antiga encontrada no índice $index e substituída.")
                } else {
                    Log.w(TAG, "substituirFotoDaFase: Uri antiga ($oldUriString) não encontrada na lista de fotos da fase.")
                }
            }
            dao.atualizarFase(fase.copy(caminhosFotos = novaListaDeFotos))
            Log.d(TAG, "substituirFotoDaFase: Fase ID: ${fase.id} atualizada no banco de dados.")
        } else {
            Log.e(TAG, "substituirFotoDaFase: Falha ao salvar a nova foto. A substituição foi cancelada.")
        }
    }
    fun girarFotoDaFase(context: Context, fase: Fase, fotoUriString: String) {
        viewModelScope.launch {
            Log.d(TAG, "girarFotoDaFase: Iniciando rotação para a foto: $fotoUriString")
            withContext(Dispatchers.IO) {
                try {
                    val fotoUri = Uri.parse(fotoUriString)
                    context.contentResolver.openInputStream(fotoUri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val matrix = Matrix().apply { postRotate(90f) }
                        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                        // Sobrescrevendo o arquivo original com o bitmap rotacionado
                        context.contentResolver.openOutputStream(fotoUri, "w")?.use { outputStream ->
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                            Log.d(TAG, "girarFotoDaFase: Bitmap rotacionado e salvo de volta na mesma Uri.")
                        }
                    }
                    // Força a UI a recompor, mesmo que a lista de URIs não mude
                    val faseAtualizada = fase.copy(caminhosFotos = fase.caminhosFotos.map { it })
                    withContext(Dispatchers.Main) {
                        dao.atualizarFase(faseAtualizada)
                        Log.d(TAG, "girarFotoDaFase: Atualização da fase forçada para refletir a rotação na UI.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "girarFotoDaFase: Erro ao girar a foto: ${e.message}", e)
                }
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
    fun exportarRelatorioDetalhadoPdf(context: Context, uri: Uri, apontamentos: List<ApontamentoCompleto>) { viewModelScope.launch(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { outputStream -> PdfExporterDetalhado.gerarPdf(context, outputStream, apontamentos) } } }
}