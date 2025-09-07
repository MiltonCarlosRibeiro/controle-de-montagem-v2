
package com.pakmatic.controledemotagem.ui.screen

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items // ✅ IMPORT ADICIONADO AQUI
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.pakmatic.controledemotagem.R
import com.pakmatic.controledemotagem.data.local.Apontamento
import com.pakmatic.controledemotagem.data.local.ApontamentoCompleto
import com.pakmatic.controledemotagem.data.local.Fase
import com.pakmatic.controledemotagem.util.CameraFileProvider
import com.pakmatic.controledemotagem.viewmodel.ApontamentoViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

//region Funções de Formatação
private fun formatTimestamp(timestamp: Long?): String { if (timestamp == null) return "--:--"; val sdf = SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault()); return sdf.format(Date(timestamp)) }
private fun formatDuration(totalSeconds: Long): String { if (totalSeconds < 0) return "0s"; val h = TimeUnit.SECONDS.toHours(totalSeconds); val m = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60; val s = totalSeconds % 60; return buildString { if (h > 0) append("${h}h "); if (m > 0) append("${m}m "); append("${s}s") } }
private fun formatCronometro(totalSeconds: Long): String { if (totalSeconds < 0) return "00:00:00"; val h = TimeUnit.SECONDS.toHours(totalSeconds); val m = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60; val s = totalSeconds % 60; return String.format("%02d:%02d:%02d", h, m, s) }
//endregion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ApontamentoScreen(viewModel: ApontamentoViewModel, navController: NavController) {
    val context = LocalContext.current
    val apontamentosCompletos by viewModel.todosApontamentos.collectAsState()
    val apontamentoParaImpedimento by viewModel.mostrarDialogImpedimento.collectAsState()
    val apontamentoParaEditar by viewModel.apontamentoParaEditar.collectAsState()
    val apontamentoParaDeletar by viewModel.apontamentoParaDeletar.collectAsState()
    val apontamentoParaNovaFase by viewModel.apontamentoParaNovaFase.collectAsState()
    val faseParaEditar by viewModel.faseParaEditar.collectAsState()
    var mostrarDialogNovoApontamento by remember { mutableStateOf(false) }
    val itemAtivo = apontamentosCompletos.find { it.apontamento.status != "Finalizado" }
    val historico = apontamentosCompletos.filter { it.apontamento.status == "Finalizado" }

    DialogImpedimento(viewModel, apontamentoParaImpedimento, navController)
    DialogEdicao(viewModel, apontamentoParaEditar)
    DialogDelecao(viewModel, apontamentoParaDeletar)
    DialogNovaFase(viewModel, apontamentoParaNovaFase)
    DialogEdicaoFase(viewModel, faseParaEditar)

    val nomeResponsavel by viewModel.nomeResponsavel.collectAsState()
    val item by viewModel.item.collectAsState()
    val descricaoItem by viewModel.descricaoItem.collectAsState()

    if (mostrarDialogNovoApontamento) { AlertDialog(onDismissRequest = { mostrarDialogNovoApontamento = false }, title = { Text("Iniciar Nova Montagem") }, text = { InputSection(nome = nomeResponsavel, onNomeChange = viewModel::onNomeResponsavelChange, item = item, onItemChange = viewModel::onItemChange, desc = descricaoItem, onDescChange = viewModel::onDescricaoItemChange) }, confirmButton = { Button(onClick = { viewModel.iniciarNovaMontagem(); mostrarDialogNovoApontamento = false }) { Text("Iniciar") } }, dismissButton = { TextButton(onClick = { mostrarDialogNovoApontamento = false }) { Text("Cancelar") } }) }
    val launcherTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri -> uri?.let { viewModel.exportarRelatorioTxt(context, it) } }
    val launcherPdfHistorico = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri -> if (uri != null && historico.isNotEmpty()) { viewModel.exportarRelatorioDetalhadoPdf(context, uri, historico) } }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.height(30.dp)) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer), actions = { IconButton(onClick = { viewModel.onNomeResponsavelChange(""); viewModel.onItemChange(""); viewModel.onDescricaoItemChange(""); mostrarDialogNovoApontamento = true }) { Icon(Icons.Default.AddCircle, contentDescription = "Iniciar Nova Montagem") }; var menuAberto by remember { mutableStateOf(false) }; IconButton(onClick = { menuAberto = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Mais opções") }; DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) { DropdownMenuItem(text = { Text("Exportar TXT (Geral)") }, onClick = { menuAberto = false; launcherTxt.launch("relatorio_geral.txt") }); DropdownMenuItem(text = { Text("Exportar PDF (Histórico)") }, enabled = historico.isNotEmpty(), onClick = { menuAberto = false; launcherPdfHistorico.launch("relatorio_historico.pdf") }) } }) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (itemAtivo != null) {
                Spacer(Modifier.height(8.dp)); CronometroAtivo(item = itemAtivo, viewModel = viewModel); Spacer(Modifier.height(16.dp)); AcoesDaMontagemAtiva(apontamento = itemAtivo.apontamento, viewModel = viewModel); Spacer(Modifier.height(16.dp)); Divider()
                GerenciadorDeFases(itemAtivo, viewModel, navController)
            } else {
                Text("Histórico de Montagens", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth())
                if (historico.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhum item finalizado.\nClique no '+' para iniciar.", color = Color.Gray) } }
                else { LazyColumn(modifier = Modifier.fillMaxSize()) { items(historico, key = { it.apontamento.id }) { item -> ApontamentoCard(item = item, viewModel = viewModel) } } }
            }
        }
    }
}


@Composable
fun AcoesDaMontagemAtiva(apontamento: Apontamento, viewModel: ApontamentoViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
        Button(onClick = { viewModel.onAbrirDialogImpedimento(apontamento) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize)); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text("Impedimento") }
        AnimatedVisibility(visible = apontamento.status == "Em Andamento" || apontamento.status == "Parado", enter = fadeIn(), exit = fadeOut()) {
            Button(onClick = { viewModel.finalizarMontagem(apontamento) }) { Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize)); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text("Finalizar") }
        }
    }
}

// ✅ ANOTAÇÕES DUPLICADAS REMOVIDAS DAQUI
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ColumnScope.GerenciadorDeFases(itemCompleto: ApontamentoCompleto, viewModel: ApontamentoViewModel, navController: NavController) {
    val context = LocalContext.current
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var faseParaFotografar by remember { mutableStateOf<Fase?>(null) }
    var fotoAntigaParaSubstituir by remember { mutableStateOf<String?>(null) } // Variável para guardar a URI antiga

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    val cameraAddLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            fotoUri?.let { uri ->
                faseParaFotografar?.let { fase ->
                    viewModel.adicionarFotoAFase(fase, uri)
                }
            }
        }
    }

    val cameraReplaceLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val novaUri = fotoUri
            val antigaUriString = fotoAntigaParaSubstituir
            val faseAtual = faseParaFotografar

            if (novaUri != null && antigaUriString != null && faseAtual != null) {
                viewModel.substituirFotoDaFase(faseAtual, antigaUriString, novaUri)
            }
        }
    }

    Text("Fases do Histórico", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth())
    if (itemCompleto.fases.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Nenhuma fase adicionada.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
    } else {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(itemCompleto.fases, key = { it.id }) { fase ->
                FaseCard(
                    fase = fase,
                    onFinalizarClick = { viewModel.finalizarFase(fase) },
                    onTirarNovaFotoClick = {
                        faseParaFotografar = fase
                        if (cameraPermissionState.status.isGranted) {
                            val uri = CameraFileProvider.getUri(context)
                            fotoUri = uri
                            cameraAddLauncher.launch(uri)
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    onSubstituirFotoClick = { fotoAntigaUri -> // Captura a URI antiga aqui
                        faseParaFotografar = fase
                        fotoAntigaParaSubstituir = fotoAntigaUri // Guarda a URI antiga
                        if (cameraPermissionState.status.isGranted) {
                            val uri = CameraFileProvider.getUri(context)
                            fotoUri = uri
                            cameraReplaceLauncher.launch(uri)
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    onGirarFotoClick = { fotoUriString -> viewModel.girarFotoDaFase(context, fase, fotoUriString) },
                    onEditClick = { viewModel.onAbrirDialogEdicaoFase(fase) },
                    onDeleteClick = { viewModel.deletarFase(fase) },
                    onDeleteFotoClick = { fotoUriString -> viewModel.deletarFotoDaFase(fase, fotoUriString) },
                    onMoverFotoClick = { fotoUriString, direcao -> viewModel.moverFoto(fase, fotoUriString, direcao) },
                    onFotoClick = { fotoUriString -> navController.navigate("fullScreenImage/${Uri.encode(fotoUriString)}") }
                )
            }
        }
    }
    Button(onClick = { viewModel.onAbrirDialogNovaFase(itemCompleto.apontamento) }, enabled = itemCompleto.apontamento.status == "Em Andamento" || itemCompleto.apontamento.status == "Parado", modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize)); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text("Adicionar Nova Fase")
    }
}

@Composable
fun FaseCard(
    fase: Fase,
    onFinalizarClick: () -> Unit,
    onTirarNovaFotoClick: () -> Unit,
    onSubstituirFotoClick: (String) -> Unit,
    onGirarFotoClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteFotoClick: (String) -> Unit,
    onMoverFotoClick: (String, Int) -> Unit,
    onFotoClick: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) { Text(fase.descricao, fontWeight = FontWeight.Bold); Text("Início: ${formatTimestamp(fase.timestampInicio)}", fontSize = 12.sp); if (fase.timestampFinal != null) { Text("Duração: ${formatDuration(fase.duracaoSegundos)}", fontSize = 12.sp, color = Color.Gray) } }
                Row {
                    if (fase.timestampFinal == null) IconButton(onClick = onFinalizarClick) { Icon(Icons.Default.CheckCircle, "Finalizar Fase", tint = Color(0xFF43A047)) }
                    IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Editar Fase", modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, "Deletar Fase", modifier = Modifier.size(20.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(fase.caminhosFotos) { index, fotoUriString ->
                    Box(modifier = Modifier.size(100.dp)) {
                        AsyncImage(model = Uri.parse(fotoUriString), contentDescription = "Foto da fase", modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small).clickable { onFotoClick(fotoUriString) }, contentScale = ContentScale.Crop)

                        // Overlay de Ações
                        Column(modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)) {
                            IconButton(onClick = { onDeleteFotoClick(fotoUriString) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, "Deletar Foto", tint = Color.White, modifier = Modifier.size(16.dp)) }
                            IconButton(onClick = { onSubstituirFotoClick(fotoUriString) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Cameraswitch, "Substituir Foto", tint = Color.White, modifier = Modifier.size(16.dp)) }
                        }

                        // Overlay de Ordenação
                        Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)), horizontalArrangement = Arrangement.SpaceEvenly) {
                            IconButton(onClick = { onMoverFotoClick(fotoUriString, -1) }, enabled = index > 0, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ArrowBack, "Mover Esquerda", tint = if (index > 0) Color.White else Color.Gray, modifier = Modifier.size(16.dp)) }
                            IconButton(onClick = { onMoverFotoClick(fotoUriString, 1) }, enabled = index < fase.caminhosFotos.size - 1, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ArrowForward, "Mover Direita", tint = if (index < fase.caminhosFotos.size - 1) Color.White else Color.Gray, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
                if (fase.timestampFinal == null) { item { Box(Modifier.size(100.dp).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small).clickable { onTirarNovaFotoClick() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.AddAPhoto, "Adicionar foto", tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            }
        }
    }
}

@Composable
fun CronometroAtivo(item: ApontamentoCompleto, viewModel: ApontamentoViewModel) {
    var tempoDecorrido by remember { mutableStateOf(0L) }
    val isCronometroPausado by viewModel.isCronometroPausado.collectAsState()

    LaunchedEffect(key1 = item.apontamento.status, isCronometroPausado) {
        val apontamentoStatus = item.apontamento.status
        if (apontamentoStatus == "Em Andamento" && !isCronometroPausado) {
            val tempoParadoTotal = item.tempoTotalParadoSegundos
            val inicio = item.apontamento.timestampInicio
            while (true) {
                val agora = System.currentTimeMillis()
                val decorridoTotal = (agora - inicio) / 1000
                tempoDecorrido = decorridoTotal - tempoParadoTotal
                delay(1000)
            }
        } else {
            val tempoParadoPrevisto = item.impedimentos.filter { it.timestampFinal != null }.sumOf { it.duracaoSegundos }
            val decorridoTotal = (System.currentTimeMillis() - item.apontamento.timestampInicio) / 1000
            tempoDecorrido = decorridoTotal - tempoParadoPrevisto
        }
    }

    val corCronometro = when { item.apontamento.status == "Em Andamento" && !isCronometroPausado -> Color(0xFF00897B); else -> Color(0xFFF57C00) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("MONTAGEM ATIVA", style = MaterialTheme.typography.titleMedium)
        Text(item.apontamento.item, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = formatCronometro(tempoDecorrido), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = corCronometro)
        Text("Status: ${item.apontamento.status}", style = MaterialTheme.typography.titleMedium, color = corCronometro)
        Spacer(Modifier.height(8.dp))
        if (item.apontamento.status != "Finalizado") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.toggleCronometro() }) { if (isCronometroPausado) { Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar Cronômetro", modifier = Modifier.size(48.dp), tint = Color(0xFF43A047)) } else { Icon(Icons.Default.Pause, contentDescription = "Pausar Cronômetro", modifier = Modifier.size(48.dp), tint = Color(0xFFF57C00)) } }
            }
        }
    }
}

@Composable
private fun ApontamentoCard(item: ApontamentoCompleto, viewModel: ApontamentoViewModel) {
    val apontamento = item.apontamento
    val context = LocalContext.current
    val launcherPdfIndividual = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri -> if (uri != null) { viewModel.exportarRelatorioDetalhadoPdf(context, uri, listOf(item)) } }
    )
    val statusColor = when(apontamento.status) { "Em Andamento" -> Color(0xFF00897B); "Parado" -> Color(0xFFF57C00); "Finalizado" -> Color.Gray; else -> Color.Black }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(text = "Item: ${apontamento.item}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "Responsável: ${apontamento.nomeResponsavel}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text("Início: ${formatTimestamp(apontamento.timestampInicio)}", fontSize = 12.sp)
            Text("Final: ${formatTimestamp(apontamento.timestampFinal)}", fontSize = 12.sp)
            Text("Tempo Parado: ${formatDuration(item.tempoTotalParadoSegundos)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Status: ${apontamento.status}", color = statusColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            if (apontamento.status == "Finalizado") {
                Spacer(Modifier.height(4.dp)); Divider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { viewModel.reabrirMontagem(apontamento) }) { Text("Reabrir") }
                    IconButton(onClick = { launcherPdfIndividual.launch("relatorio_detalhado_${apontamento.item}.pdf") }) { Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar Relatório do Item") }
                    IconButton(onClick = { viewModel.onAbrirDialogEdicao(apontamento) }) { Icon(Icons.Default.Edit, "Editar") }
                    IconButton(onClick = { viewModel.onAbrirDialogDelecao(apontamento) }) { Icon(Icons.Default.Delete, "Deletar") }
                }
            }
        }
    }
}

@Composable
private fun InputSection(nome: String, onNomeChange: (String) -> Unit, item: String, onItemChange: (String) -> Unit, desc: String, onDescChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = nome, onValueChange = onNomeChange, label = { Text("Nome do Responsável") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = item, onValueChange = onItemChange, label = { Text("Item") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = onDescChange, label = { Text("Descrição do Item") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun DialogImpedimento(viewModel: ApontamentoViewModel, apontamento: Apontamento?, navController: NavController) {
    if (apontamento != null) {
        val descricaoImpedimento by viewModel.descricaoImpedimento.collectAsState()
        AlertDialog(
            onDismissRequest = { viewModel.onFecharDialogImpedimento() },
            title = { Text("Registrar Impedimento") },
            text = { OutlinedTextField(value = descricaoImpedimento, onValueChange = viewModel::onDescricaoImpedimentoChange, label = { Text("Motivo da parada") }) },
            confirmButton = { Button(onClick = { viewModel.registrarImpedimento(); viewModel.onFecharDialogImpedimento(); navController.popBackStack() }) { Text("Pausar e Voltar") } },
            dismissButton = { TextButton(onClick = { viewModel.onFecharDialogImpedimento() }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogEdicao(viewModel: ApontamentoViewModel, apontamentoParaEditar: Apontamento?) {
    if (apontamentoParaEditar == null) return
    var showDatePickerInicio by remember { mutableStateOf(false) }
    var showTimePickerInicio by remember { mutableStateOf(false) }
    var showDatePickerFinal by remember { mutableStateOf(false) }
    var showTimePickerFinal by remember { mutableStateOf(false) }
    val nome by viewModel.nomeResponsavel.collectAsState()
    val item by viewModel.item.collectAsState()
    val desc by viewModel.descricaoItem.collectAsState()
    val dataInicio by viewModel.dataInicioEdit.collectAsState()
    val horaInicio by viewModel.horaInicioEdit.collectAsState()
    val dataFinal by viewModel.dataFinalEdit.collectAsState()
    val horaFinal by viewModel.horaFinalEdit.collectAsState()
    val datePickerStateInicio = rememberDatePickerState(initialSelectedDateMillis = apontamentoParaEditar.timestampInicio)
    if (showDatePickerInicio) { DatePickerDialog(onDismissRequest = { showDatePickerInicio = false }, confirmButton = { TextButton(onClick = { showDatePickerInicio = false; datePickerStateInicio.selectedDateMillis?.let { val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); viewModel.onDataInicioChange(sdf.format(Date(it))) } }) { Text("OK") } }) { DatePicker(state = datePickerStateInicio) } }
    val calendarInicio = Calendar.getInstance().apply { timeInMillis = apontamentoParaEditar.timestampInicio }
    val timePickerStateInicio = rememberTimePickerState(initialHour = calendarInicio.get(Calendar.HOUR_OF_DAY), initialMinute = calendarInicio.get(Calendar.MINUTE))
    if (showTimePickerInicio) { AlertDialog(onDismissRequest = { showTimePickerInicio = false }, title = { Text("Selecione a Hora") }, text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center){ TimePicker(state = timePickerStateInicio) }}, confirmButton = { TextButton(onClick = { showTimePickerInicio = false; viewModel.onHoraInicioChange(String.format("%02d:%02d", timePickerStateInicio.hour, timePickerStateInicio.minute)) }) { Text("OK") } }) }
    val calendarFinal = Calendar.getInstance().apply { timeInMillis = apontamentoParaEditar.timestampFinal ?: System.currentTimeMillis() }
    val datePickerStateFinal = rememberDatePickerState(initialSelectedDateMillis = apontamentoParaEditar.timestampFinal)
    if (showDatePickerFinal) { DatePickerDialog(onDismissRequest = { showDatePickerFinal = false }, confirmButton = { TextButton(onClick = { showDatePickerFinal = false; datePickerStateFinal.selectedDateMillis?.let { val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); viewModel.onDataFinalChange(sdf.format(Date(it))) } }) { DatePicker(state = datePickerStateFinal) } }) { DatePicker(state = datePickerStateFinal) } }
    val timePickerStateFinal = rememberTimePickerState(initialHour = calendarFinal.get(Calendar.HOUR_OF_DAY), initialMinute = calendarFinal.get(Calendar.MINUTE))
    if (showTimePickerFinal) { AlertDialog(onDismissRequest = { showTimePickerFinal = false }, title = { Text("Selecione a Hora") }, text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center){ TimePicker(state = timePickerStateFinal) }}, confirmButton = { TextButton(onClick = { showTimePickerFinal = false; viewModel.onHoraFinalChange(String.format("%02d:%02d", timePickerStateFinal.hour, timePickerStateFinal.minute)) }) { Text("OK") } }) }

    AlertDialog(onDismissRequest = { viewModel.onFecharDialogEdicao() }, title = { Text("Editar Apontamento") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = nome, onValueChange = viewModel::onNomeResponsavelChange, label = { Text("Nome do Responsável") })
            OutlinedTextField(value = item, onValueChange = viewModel::onItemChange, label = { Text("Item") })
            OutlinedTextField(value = desc, onValueChange = viewModel::onDescricaoItemChange, label = { Text("Descrição do Item") })
            Spacer(Modifier.height(8.dp)); Divider()
            Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = dataInicio, onValueChange = {}, readOnly = true, label = { Text("Data Início") }, modifier = Modifier.weight(1f)); IconButton(onClick = { showDatePickerInicio = true }) { Icon(Icons.Default.CalendarToday, "Calendário") } }
            Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = horaInicio, onValueChange = {}, readOnly = true, label = { Text("Hora Início") }, modifier = Modifier.weight(1f)); IconButton(onClick = { showTimePickerInicio = true }) { Icon(Icons.Default.AccessTime, "Relógio") } }
            Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = dataFinal, onValueChange = {}, readOnly = true, label = { Text("Data Final") }, modifier = Modifier.weight(1f)); IconButton(onClick = { showDatePickerFinal = true }) { Icon(Icons.Default.CalendarToday, "Calendário") } }
            Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = horaFinal, onValueChange = {}, readOnly = true, label = { Text("Hora Final") }, modifier = Modifier.weight(1f)); IconButton(onClick = { showTimePickerFinal = true }) { Icon(Icons.Default.AccessTime, "Relógio") } }
        }
    }, confirmButton = { Button(onClick = { viewModel.salvarEdicao() }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = { viewModel.onFecharDialogEdicao() }) { Text("Cancelar") } })
}

@Composable
fun DialogDelecao(viewModel: ApontamentoViewModel, apontamento: Apontamento?) { if (apontamento != null) { AlertDialog(onDismissRequest = { viewModel.onFecharDialogDelecao() }, title = { Text("Confirmar Exclusão") }, text = { Text("Tem certeza que deseja deletar o item '${apontamento.item}'? Esta ação não pode ser desfeita.") }, confirmButton = { Button(onClick = { viewModel.confirmarDelecao() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Deletar") } }, dismissButton = { TextButton(onClick = { viewModel.onFecharDialogDelecao() }) { Text("Cancelar") } }) } }

@Composable
fun DialogNovaFase(viewModel: ApontamentoViewModel, apontamento: Apontamento?) {
    if (apontamento != null) {
        val descricaoFase by viewModel.descricaoFase.collectAsState()
        AlertDialog(
            onDismissRequest = { viewModel.onFecharDialogNovaFase() },
            title = { Text("Adicionar Nova Fase") },
            text = { OutlinedTextField(value = descricaoFase, onValueChange = viewModel::onDescricaoFaseChange, label = { Text("Descrição da fase") }, modifier = Modifier.fillMaxWidth().height(150.dp), singleLine = false) },
            confirmButton = { Button(onClick = { viewModel.iniciarNovaFase() }) { Text("Iniciar Fase") } },
            dismissButton = { TextButton(onClick = { viewModel.onFecharDialogNovaFase() }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun DialogEdicaoFase(viewModel: ApontamentoViewModel, faseParaEditar: Fase?) {
    if (faseParaEditar == null) return
    val descricaoFase by viewModel.descricaoFase.collectAsState()
    AlertDialog(
        onDismissRequest = { viewModel.onFecharDialogEdicaoFase() },
        title = { Text("Editar Fase") },
        text = { OutlinedTextField(value = descricaoFase, onValueChange = viewModel::onDescricaoFaseChange, label = { Text("Descrição da fase") }, modifier = Modifier.fillMaxWidth().height(150.dp), singleLine = false) },
        confirmButton = { Button(onClick = { viewModel.salvarEdicaoFase() }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { viewModel.onFecharDialogEdicaoFase() }) { Text("Cancelar") } }
    )
}