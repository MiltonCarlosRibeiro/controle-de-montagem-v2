package com.pakmatic.controledemotagem.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pakmatic.controledemotagem.R
import com.pakmatic.controledemotagem.data.local.Apontamento
import com.pakmatic.controledemotagem.data.local.ApontamentoComImpedimentos
import com.pakmatic.controledemotagem.viewmodel.ApontamentoViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// (Funções de formatação permanecem as mesmas)
private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return "--:--"
    val sdf = SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
private fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds < 0) return "0s"
    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append("${hours}h ")
        if (minutes > 0) append("${minutes}m ")
        append("${seconds}s")
    }
}
private fun formatCronometro(totalSeconds: Long): String {
    if (totalSeconds < 0) return "00:00:00"
    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApontamentoScreen(viewModel: ApontamentoViewModel) {
    val context = LocalContext.current
    val nomeResponsavel by viewModel.nomeResponsavel.collectAsState()
    val item by viewModel.item.collectAsState()
    val descricaoItem by viewModel.descricaoItem.collectAsState()
    val apontamentosComImpedimentos by viewModel.todosApontamentos.collectAsState()

    val apontamentoParaImpedimento by viewModel.mostrarDialogImpedimento.collectAsState()
    val apontamentoParaEditar by viewModel.apontamentoParaEditar.collectAsState()
    val apontamentoParaDeletar by viewModel.apontamentoParaDeletar.collectAsState()

    val itemAtivo = apontamentosComImpedimentos.find { it.apontamento.status == "Em Andamento" || it.apontamento.status == "Parado" }

    DialogImpedimento(viewModel, apontamentoParaImpedimento)
    DialogEdicao(viewModel, apontamentoParaEditar, nomeResponsavel, item, descricaoItem)
    DialogDelecao(viewModel, apontamentoParaDeletar)

    val launcherTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri -> uri?.let { viewModel.exportarRelatorioTxt(context, it) } }
    val launcherPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uri?.let { viewModel.exportarRelatorioPdf(context, it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.height(30.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    var menuAberto by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuAberto = true }) { Icon(Icons.Default.MoreVert, "Opções") }
                    DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                        DropdownMenuItem(text = { Text("Exportar .TXT") }, onClick = { menuAberto = false; launcherTxt.launch("relatorio.txt") })
                        DropdownMenuItem(text = { Text("Exportar .PDF") }, onClick = { menuAberto = false; launcherPdf.launch("relatorio.pdf") })
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (itemAtivo == null) {
                Text("Iniciar Nova Montagem", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                InputSection(nomeResponsavel, viewModel::onNomeResponsavelChange, item, viewModel::onItemChange, descricaoItem, viewModel::onDescricaoItemChange, viewModel::iniciarNovaMontagem)
            } else {
                CronometroAtivo(item = itemAtivo)
            }

            Spacer(Modifier.height(16.dp)); Divider(); Spacer(Modifier.height(16.dp))

            Text("Histórico de Montagens", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            ApontamentosList(lista = apontamentosComImpedimentos, viewModel = viewModel)
        }
    }
}

// (CronometroAtivo permanece o mesmo)
@Composable
fun CronometroAtivo(item: ApontamentoComImpedimentos) {
    var tempoDecorrido by remember { mutableStateOf(0L) }

    LaunchedEffect(key1 = item.apontamento.status) {
        if (item.apontamento.status == "Em Andamento") {
            val tempoParadoTotal = item.tempoTotalParadoSegundos
            val inicio = item.apontamento.timestampInicio
            while (true) {
                val agora = System.currentTimeMillis()
                val decorridoTotal = (agora - inicio) / 1000
                tempoDecorrido = decorridoTotal - tempoParadoTotal
                delay(1000)
            }
        } else { // Parado
            val tempoParadoPrevisto = item.impedimentos.filter { it.timestampFinal != null }.sumOf { it.duracaoSegundos }
            val decorridoTotal = (System.currentTimeMillis() - item.apontamento.timestampInicio) / 1000
            tempoDecorrido = decorridoTotal - tempoParadoPrevisto
        }
    }

    val corCronometro = if (item.apontamento.status == "Em Andamento") Color(0xFF00897B) else Color(0xFFF57C00)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("MONTAGEM EM ANDAMENTO", style = MaterialTheme.typography.titleMedium)
        Text(item.apontamento.item, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = formatCronometro(tempoDecorrido),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = corCronometro
        )
        Text("Status: ${item.apontamento.status}", style = MaterialTheme.typography.titleMedium, color = corCronometro)
    }
}

@Composable
private fun ApontamentosList(
    lista: List<ApontamentoComImpedimentos>,
    viewModel: ApontamentoViewModel
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // <<< CORREÇÃO AQUI
        // Adicionamos uma 'key' única para cada item, usando o ID do banco de dados.
        // Isso melhora a performance e evita erros de renderização.
        items(
            items = lista,
            key = { item -> item.apontamento.id }
        ) { item ->
            ApontamentoCard(item = item, viewModel = viewModel)
        }
    }
}


@Composable
private fun ApontamentoCard(item: ApontamentoComImpedimentos, viewModel: ApontamentoViewModel) {
    val apontamento = item.apontamento
    val statusColor = when(apontamento.status) {
        "Em Andamento" -> Color(0xFF00897B)
        "Parado" -> Color(0xFFF57C00)
        "Finalizado" -> Color.Gray
        else -> Color.Black
    }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(text = "Item: ${apontamento.item}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "Responsável: ${apontamento.nomeResponsavel}")
            Spacer(Modifier.height(8.dp))
            Text("Início: ${formatTimestamp(apontamento.timestampInicio)}")
            Text("Final: ${formatTimestamp(apontamento.timestampFinal)}")
            Text("Tempo Parado: ${formatDuration(item.tempoTotalParadoSegundos)}", fontWeight = FontWeight.Bold)
            Text("Status: ${apontamento.status}", color = statusColor, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (apontamento.status == "Em Andamento") {
                        Button(onClick = { viewModel.onAbrirDialogImpedimento(apontamento) }) { Text("Impedimento") }
                        Button(onClick = { viewModel.finalizarMontagem(apontamento) }) { Text("Finalizar") }
                    }
                    if (apontamento.status == "Parado") {
                        Button(onClick = { viewModel.retomarTrabalho(apontamento) }, colors = ButtonDefaults.buttonColors(Color(0xFF43A047))) { Text("Retomar") }
                    }
                }
                if (apontamento.status != "Em Andamento" && apontamento.status != "Parado") {
                    Row {
                        IconButton(onClick = { viewModel.onAbrirDialogEdicao(apontamento) }) { Icon(Icons.Default.Edit, "Editar") }
                        IconButton(onClick = { viewModel.onAbrirDialogDelecao(apontamento) }) { Icon(Icons.Default.Delete, "Deletar") }
                    }
                }
            }
        }
    }
}

// (InputSection e Dialogs permanecem os mesmos)
@Composable
private fun InputSection(nome: String, onNomeChange: (String) -> Unit, item: String, onItemChange: (String) -> Unit, desc: String, onDescChange: (String) -> Unit, onIniciarClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = nome, onValueChange = onNomeChange, label = { Text("Nome do Responsável") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = item, onValueChange = onItemChange, label = { Text("Item") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = onDescChange, label = { Text("Descrição do Item") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onIniciarClick, modifier = Modifier.fillMaxWidth()) { Text("INICIAR MONTAGEM") }
    }
}

@Composable
fun DialogImpedimento(viewModel: ApontamentoViewModel, apontamento: Apontamento?) {
    if (apontamento != null) {
        val descricaoImpedimento by viewModel.descricaoImpedimento.collectAsState()
        AlertDialog(
            onDismissRequest = { viewModel.onFecharDialogImpedimento() },
            title = { Text("Registrar Impedimento") },
            text = { OutlinedTextField(value = descricaoImpedimento, onValueChange = viewModel::onDescricaoImpedimentoChange, label = { Text("Motivo da parada") }) },
            confirmButton = { Button(onClick = { viewModel.registrarImpedimento() }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { viewModel.onFecharDialogImpedimento() }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun DialogEdicao(viewModel: ApontamentoViewModel, apontamento: Apontamento?, nome: String, item: String, desc: String) {
    if (apontamento != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onFecharDialogEdicao() },
            title = { Text("Editar Apontamento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nome, onValueChange = viewModel::onNomeResponsavelChange, label = { Text("Nome do Responsável") })
                    OutlinedTextField(value = item, onValueChange = viewModel::onItemChange, label = { Text("Item") })
                    OutlinedTextField(value = desc, onValueChange = viewModel::onDescricaoItemChange, label = { Text("Descrição do Item") })
                }
            },
            confirmButton = { Button(onClick = { viewModel.salvarEdicao() }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { viewModel.onFecharDialogEdicao() }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun DialogDelecao(viewModel: ApontamentoViewModel, apontamento: Apontamento?) {
    if (apontamento != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onFecharDialogDelecao() },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Tem certeza que deseja deletar o item '${apontamento.item}'? Esta ação não pode ser desfeita.") },
            confirmButton = { Button(onClick = { viewModel.confirmarDelecao() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Deletar") } },
            dismissButton = { TextButton(onClick = { viewModel.onFecharDialogDelecao() }) { Text("Cancelar") } }
        )
    }
}