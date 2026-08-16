package com.cactus.demo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CactusDemoScreen()
            }
        }
    }
}

@Composable
fun CactusDemoScreen() {
    var status by remember { mutableStateOf("Not initialized") }
    var modelDir by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cactus Engine Demo", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = modelDir,
            onValueChange = { modelDir = it },
            label = { Text("Model directory path") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                if (modelDir.isBlank()) {
                    Toast.makeText(context, "Enter a model directory path", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                busy = true
                status = "Initializing..."
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { CactusEngine.init(modelDir) }
                        status = "Ready"
                        Toast.makeText(context, "Engine initialized", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        status = "Init failed: ${e.message}"
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (busy) "Working..." else "Initialize Engine")
        }

        Text("Status: $status", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Prompt") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Button(
            onClick = {
                if (prompt.isBlank()) return@Button
                busy = true
                response = "Thinking..."
                scope.launch {
                    try {
                        val messages = """[{"role":"user","content":"${prompt.replace("\"", "\\\"")}"}]"""
                        val result = withContext(Dispatchers.IO) {
                            CactusEngine.complete(messages, """{"max_tokens":128}""")
                        }
                        response = result
                    } catch (e: Exception) {
                        response = "Error: ${e.message}"
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && CactusEngine.isLoaded,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (busy) "Running..." else "Run Completion")
        }

        Text("Response:", style = MaterialTheme.typography.titleMedium)
        Text(
            response,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
