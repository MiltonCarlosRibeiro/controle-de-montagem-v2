package com.pakmatic.controledemotagem.ui.screen

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return "--:--"
    val sdf = SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
private fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds < 0) return "0s"
    val hours = TimeUnit.SECONDS.toHours(totalSeconds); val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60; val seconds = totalSeconds % 60
    return buildString { if (hours > 0) append("${hours}h "); if (minutes > 0) append("${minutes}m "); append("${seconds}s") }
}
private fun formatCronometro(totalSeconds: Long): String {
    if (totalSeconds < 0) return "00:00:00"
    val hours = TimeUnit.SECONDS.toHours(totalSeconds); val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60; val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
//endregion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ApontamentoScreen(viewModel: ApontamentoViewModel) {
    val context = LocalContext.current
    val nomeResponsavel by viewModel.nomeResponsavel.collectAsState()
    val item by viewModel.item.collectAsState()
    val descricaoItem by viewModel.descricaoItem.collectAsState()
    val apontamentosCompletos by viewModel.todosApontamentos.collectAsState()

    val apontamentoParaImpedimento by viewModel.mostrarDialogImpedimento.collectAsState()
    val apontamentoParaEditar by viewModel.apontamentoParaEditar.collectAsState()
    val apontamentoParaDeletar by viewModel.apontamentoParaDeletar.collectAsState()
    val apontamentoParaNovaFase by viewModel.apontamentoParaNovaFase.collectAsState()
    var mostrarDialogNovoApontamento by remember { mutableStateOf(false) }

    val itemAtivo = apontamentosCompletos.find { it.apontamento.status != "Finalizado" }

    // <<< CORREÇÃO: A lista de histórico agora é separada da lógica do item ativo >>>
    val historico = apontamentosCompletos.filter { it.apontamento.status == "Finalizado" }

    DialogImpedimento(viewModel, apontamentoParaImpedimento)
    DialogEdicao(viewModel, apontamentoParaEditar, nomeResponsavel, item, descricaoItem)
    DialogDelecao(viewModel, apontamentoParaDeletar)
    DialogNovaFase(viewModel, apontamentoParaNovaFase)

    if (mostrarDialogNovoApontamento) {
        AlertDialog(
            onDismissRequest = { mostrarDialogNovoApontamento = false },
            title = { Text("Iniciar Nova Montagem") },
            text = { InputSection(nome = nomeResponsavel, onNomeChange = viewModel::onNomeResponsavelChange, item = item, onItemChange = viewModel::onItemChange, desc = descricaoItem, onDescChange = viewModel::onDescricaoItemChange) },
            confirmButton = { Button(onClick = { viewModel.iniciarNovaMontagem(); mostrarDialogNovoApontamento = false }) { Text("Iniciar") } },
            dismissButton = { TextButton(onClick = { mostrarDialogNovoApontamento = false }) { Text("Cancelar") } }
        )
    }

    val launcherTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri -> uri?.let { viewModel.exportarRelatorioTxt(context, it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.height(30.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    IconButton(onClick = {
                        viewModel.onNomeResponsavelChange(""); viewModel.onItemChange(""); viewModel.onDescricaoItemChange("")
                        mostrarDialogNovoApontamento = true
                    }) { Icon(Icons.Default.AddCircle, contentDescription = "Iniciar Nova Montagem") }

                    var menuAberto by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuAberto = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Mais opções") }
                    DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                        DropdownMenuItem(
                            text = { Text("Exportar TXT (Geral)") },
                            onClick = { menuAberto = false; launcherTxt.launch("relatorio_geral.txt") }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (itemAtivo != null) {
                Spacer(Modifier.height(8.dp)); CronometroAtivo(item = itemAtivo); Spacer(Modifier.height(16.dp))
                AcoesDaMontagemAtiva(apontamento = itemAtivo.apontamento, viewModel = viewModel)
                Spacer(Modifier.height(16.dp)); Divider()
                GerenciadorDeFases(itemAtivo, viewModel)
            } else {
                // <<< CORREÇÃO: O histórico agora é a visão principal quando não há item ativo >>>
                Text("Histórico de Montagens", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth())
                if (historico.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum item finalizado.\nClique no '+' para iniciar.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(historico, key = { it.apontamento.id }) { item ->
                            ApontamentoCard(item = item, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}


// O restante do código (AcoesDaMontagemAtiva, ApontamentoCard, GerenciadorDeFases, etc.)
// permanece o mesmo. Colei tudo abaixo para garantir.

@Composable
fun AcoesDaMontagemAtiva(apontamento: Apontamento, viewModel: ApontamentoViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        if (apontamento.status == "Em Andamento") {
            Button(onClick = { viewModel.onAbrirDialogImpedimento(apontamento) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize)); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text("Impedimento")
            }
            Button(onClick = { viewModel.finalizarMontagem(apontamento) }) {
                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize)); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text("Finalizar")
            }
        }
        if (apontamento.status == "Parado") {
            Button(onClick = { viewModel.retomarTrabalho(apontamento) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize)); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text("Retomar")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ColumnScope.GerenciadorDeFases(itemCompleto: ApontamentoCompleto, viewModel: ApontamentoViewModel) {
    val context = LocalContext.current
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var faseParaFotografar by remember { mutableStateOf<Fase?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { fotoUri?.let { uri -> faseParaFotografar?.let { fase -> viewModel.salvarCaminhoFoto(fase, uri) } } }
    }
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    Text("Fases do Processo", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth())
    LazyColumn(modifier = Modifier.weight(1f)) {
        items(itemCompleto.fases, key = { it.id }) { fase ->
            FaseCard(fase = fase, onFinalizarClick = { viewModel.finalizarFase(fase) }, onTirarFotoClick = {
                if (cameraPermissionState.status.isGranted) {
                    faseParaFotografar = fase; val uri = CameraFileProvider.getUri(context); fotoUri = uri; cameraLauncher.launch(uri)
                } else { cameraPermissionState.launchPermissionRequest() }
            })
        }
    }
    Button(
        onClick = { viewModel.onAbrirDialogNovaFase(itemCompleto.apontamento) },
        enabled = itemCompleto.apontamento.status == "Em Andamento",
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("Adicionar Nova Fase")
    }
}

@Composable
fun FaseCard(fase: Fase, onFinalizarClick: () -> Unit, onTirarFotoClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (fase.caminhoFoto != null) {
                AsyncImage(model = Uri.parse(fase.caminhoFoto), contentDescription = "Foto da fase", modifier = Modifier.size(60.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.size(60.dp).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Image, "Sem foto", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fase.descricao, fontWeight = FontWeight.Bold)
                Text("Início: ${formatTimestamp(fase.timestampInicio)}", fontSize = 12.sp)
                if (fase.timestampFinal != null) { Text("Duração: ${formatDuration(fase.duracaoSegundos)}", fontSize = 12.sp, color = Color.Gray) }
            }
            if (fase.timestampFinal == null) {
                Row {
                    IconButton(onClick = onTirarFotoClick) { Icon(Icons.Default.PhotoCamera, "Tirar Foto") }
                    IconButton(onClick = onFinalizarClick) { Icon(Icons.Default.CheckCircle, "Finalizar Fase", tint = Color(0xFF43A047)) }
                }
            }
        }
    }
}

@Composable
fun CronometroAtivo(item: ApontamentoCompleto) {
    var tempoDecorrido by remember { mutableStateOf(0L) }
    LaunchedEffect(key1 = item.apontamento.status) {
        if (item.apontamento.status == "Em Andamento") {
            val tempoParadoTotal = item.tempoTotalParadoSegundos; val inicio = item.apontamento.timestampInicio
            while (true) { val agora = System.currentTimeMillis(); val decorridoTotal = (agora - inicio) / 1000; tempoDecorrido = decorridoTotal - tempoParadoTotal; delay(1000) }
        } else {
            val tempoParadoPrevisto = item.impedimentos.filter { it.timestampFinal != null }.sumOf { it.duracaoSegundos }
            val decorridoTotal = (System.currentTimeMillis() - item.apontamento.timestampInicio) / 1000
            tempoDecorrido = decorridoTotal - tempoParadoPrevisto
        }
    }
    val corCronometro = if (item.apontamento.status == "Em Andamento") Color(0xFF00897B) else Color(0xFFF57C00)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("MONTAGEM ATIVA", style = MaterialTheme.typography.titleMedium)
        Text(item.apontamento.item, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = formatCronometro(tempoDecorrido), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = corCronometro)
        Text("Status: ${item.apontamento.status}", style = MaterialTheme.typography.titleMedium, color = corCronometro)
    }
}

@Composable
private fun ApontamentoCard(item: ApontamentoCompleto, viewModel: ApontamentoViewModel) {
    val apontamento = item.apontamento
    val context = LocalContext.current
    val launcherPdfDetalhado = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri -> uri?.let { viewModel.exportarRelatorioDetalhadoPdf(context, it, item) } }
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
                    Text("Gerenciar:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    IconButton(onClick = { launcherPdfDetalhado.launch("relatorio_detalhado_${apontamento.item}.pdf") }) { Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar Relatório Detalhado") }
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
fun DialogImpedimento(viewModel: ApontamentoViewModel, apontamento: Apontamento?) {
    if (apontamento != null) {
        val descricaoImpedimento by viewModel.descricaoImpedimento.collectAsState()
        AlertDialog(onDismissRequest = { viewModel.onFecharDialogImpedimento() }, title = { Text("Registrar Impedimento") }, text = { OutlinedTextField(value = descricaoImpedimento, onValueChange = viewModel::onDescricaoImpedimentoChange, label = { Text("Motivo da parada") }) }, confirmButton = { Button(onClick = { viewModel.registrarImpedimento() }) { Text("Confirmar") } }, dismissButton = { TextButton(onClick = { viewModel.onFecharDialogImpedimento() }) { Text("Cancelar") } })
    }
}

@Composable
fun DialogEdicao(viewModel: ApontamentoViewModel, apontamento: Apontamento?, nome: String, item: String, desc: String) {
    if (apontamento != null) {
        AlertDialog(onDismissRequest = { viewModel.onFecharDialogEdicao() }, title = { Text("Editar Apontamento") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = nome, onValueChange = viewModel::onNomeResponsavelChange, label = { Text("Nome do Responsável") }); OutlinedTextField(value = item, onValueChange = viewModel::onItemChange, label = { Text("Item") }); OutlinedTextField(value = desc, onValueChange = viewModel::onDescricaoItemChange, label = { Text("Descrição do Item") }) } }, confirmButton = { Button(onClick = { viewModel.salvarEdicao() }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = { viewModel.onFecharDialogEdicao() }) { Text("Cancelar") } })
    }
}

@Composable
fun DialogDelecao(viewModel: ApontamentoViewModel, apontamento: Apontamento?) {
    if (apontamento != null) {
        AlertDialog(onDismissRequest = { viewModel.onFecharDialogDelecao() }, title = { Text("Confirmar Exclusão") }, text = { Text("Tem certeza que deseja deletar o item '${apontamento.item}'? Esta ação não pode ser desfeita.") }, confirmButton = { Button(onClick = { viewModel.confirmarDelecao() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Deletar") } }, dismissButton = { TextButton(onClick = { viewModel.onFecharDialogDelecao() }) { Text("Cancelar") } })
    }
}

@Composable
fun DialogNovaFase(viewModel: ApontamentoViewModel, apontamento: Apontamento?) {
    if (apontamento != null) {
        val descricaoFase by viewModel.descricaoFase.collectAsState()
        AlertDialog(onDismissRequest = { viewModel.onFecharDialogNovaFase() }, title = { Text("Adicionar Nova Fase") }, text = { OutlinedTextField(value = descricaoFase, onValueChange = viewModel::onDescricaoFaseChange, label = { Text("Descrição da fase") }, modifier = Modifier.fillMaxWidth()) }, confirmButton = { Button(onClick = { viewModel.iniciarNovaFase() }) { Text("Iniciar Fase") } }, dismissButton = { TextButton(onClick = { viewModel.onFecharDialogNovaFase() }) { Text("Cancelar") } })
    }
}