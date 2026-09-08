package com.example.converanything.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.converanything.core.ConversionJob
import com.example.converanything.core.JobStatus
import com.example.converanything.data.ConvertApp
import com.example.converanything.native.EngineBridge

@Composable
fun QueueScreen() {
    val app = LocalContext.current.applicationContext as ConvertApp
    val live by EngineBridge.jobs.collectAsState()
    val stored by app.history.entries.collectAsState()
    val toast = LocalToast.current
    var detail by remember { mutableStateOf<ConversionJob?>(null) }
    LaunchedEffect(Unit) {
        app.errors.collect { toast.error(it) }
    }
    val active = live.filter { it.status == JobStatus.QUEUED || it.status == JobStatus.RUNNING }
    val liveDone = live.filter { it.status == JobStatus.DONE || it.status == JobStatus.FAILED }
    val history = liveDone + stored.filter { s -> liveDone.none { it.id == s.id } }

    LazyColumn(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp)) {
        item {
            Spacer(Modifier.height(12.dp))
            ScreenHeader(
                "Queue",
                if (active.isEmpty()) "Idle"
                else "${active.count { it.status == JobStatus.RUNNING }} running · " +
                    "${active.count { it.status == JobStatus.QUEUED }} waiting",
            )
            Spacer(Modifier.height(8.dp))
        }

        if (active.isNotEmpty()) {
            item(key = "h-active") { SectionHead("Active", null) }
            val batches = active.groupBy { it.batchId }
            batches.forEach { (batchId, group) ->
                if (group.size > 1) {
                    item(key = "ah-$batchId") {
                        Text(
                            "Batch of ${group.size}",
                            color = Blue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
                items(group, key = { it.id }) { j ->
                    JobRow(job = j, library = app.library, onCancel = { EngineBridge.cancel(j.id) })
                }
            }
        }

        item(key = "h-history") {
            SectionHead("History · ${history.size}") {
                if (history.isNotEmpty()) {
                    TextButton(onClick = {
                        app.history.clear()
                        EngineBridge.clearDone()
                    }) { Text("Clear", color = Mist) }
                }
            }
        }
        if (history.isEmpty()) {
            item(key = "h-empty") {
                EmptyState(
                    "No history yet",
                    "Finished conversions stay listed across restarts.",
                )
            }
        } else {
            items(history, key = { "hist-${it.id}" }) { j ->
                HistoryRow(
                    job = j,
                    library = app.library,
                    onTap = { detail = j },
                    onDelete = {
                        app.history.remove(j.id)
                        EngineBridge.remove(j.id)
                    },
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    detail?.let { j ->
        QueryDetail(
            job = j,
            subfolder = runCatching { EngineBridge.type(j.typeId).outputSubfolder }.getOrNull(),
            onRetry = {
                val t = runCatching { EngineBridge.type(j.typeId) }.getOrNull()
                if (t == null) {
                    toast.error("Format list unavailable. Reopen the tab.")
                } else {
                    app.enqueue(
                        j.typeId, listOf(j.source), j.outputFormat.extension,
                        com.example.converanything.core.Destination.Default(t),
                    )
                    detail = null
                }
            },
            onClose = { detail = null },
        )
    }
}

@Composable
private fun SectionHead(title: String, action: @Composable (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(),
            color = Mist,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun JobRow(
    job: ConversionJob,
    library: com.example.converanything.data.MediaLibrary,
    onCancel: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumb(job.source, library, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    job.source.displayName,
                    color = Snow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    "to ${job.outputFormat.extension.uppercase()} · ${job.status.name.lowercase()}" +
                        (job.error?.let { " · ${mapUserError(it)}" } ?: ""),
                    color = Mist,
                    fontSize = 12.sp,
                    maxLines = 2,
                )
            }
            Text(
                "${job.progress}%",
                color = if (job.status == JobStatus.DONE) Success else Snow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { job.progress / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = when (job.status) {
                JobStatus.DONE -> Success
                JobStatus.FAILED -> Danger
                else -> Blue
            },
            trackColor = Hairline,
        )
        Spacer(Modifier.height(8.dp))
        OrayvaGhostButton("Cancel", onClick = onCancel)
    }
}

@Composable
private fun HistoryRow(
    job: ConversionJob,
    library: com.example.converanything.data.MediaLibrary,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val view = LocalView.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .pressable(onClick = onTap)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumb(job.source, library, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                job.source.displayName,
                color = Snow,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                "to ${job.outputFormat.extension.uppercase()} · " +
                    if (job.status == JobStatus.DONE) "done"
                    else "failed" + (job.error?.let { " · ${mapUserError(it)}" } ?: ""),
                color = if (job.status == JobStatus.DONE) Success else Danger,
                fontSize = 12.sp,
                maxLines = 2,
            )
        }
        TextButton(onClick = {
            view.warn()
            onDelete()
        }) { Text("Remove", color = Mist, fontSize = 12.sp) }
    }
}

@Composable
private fun QueryDetail(
    job: ConversionJob,
    subfolder: String?,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val done = job.status == JobStatus.DONE
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Surface,
        title = { Text(job.source.displayName, color = Snow) },
        text = {
            Column {
                Text(
                    "${job.source.extension.uppercase().ifBlank { "FILE" }}  to  " +
                        job.outputFormat.extension.uppercase(),
                    color = Snow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (done) "Done · ${job.progress}%"
                    else if (job.status == JobStatus.FAILED) "Failed"
                    else "${job.status.name.lowercase()} · ${job.progress}%",
                    color = if (done) Success else if (job.status == JobStatus.FAILED) Danger else Blue,
                    fontSize = 13.sp,
                )
                job.error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(mapUserError(it), color = Mist, fontSize = 13.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (done && subfolder != null) "Downloads/Orayva/$subfolder/"
                    else if (done) "Conversion completed."
                    else "No output file was produced.",
                    color = Mist,
                    fontSize = 13.sp,
                )
            }
        },
        confirmButton = {
            if (!done && job.status != JobStatus.FAILED) {
                TextButton(onClick = onClose) { Text("Close", color = Mist) }
            } else {
                TextButton(onClick = onRetry) {
                    Text(if (done) "Convert again" else "Retry", color = Blue)
                }
            }
        },
        dismissButton = {
            if (done || job.status == JobStatus.FAILED) {
                TextButton(onClick = onClose) { Text("Close", color = Mist) }
            }
        },
    )
}
