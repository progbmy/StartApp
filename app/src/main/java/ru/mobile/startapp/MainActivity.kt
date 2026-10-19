package ru.mobile.startapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.mobile.startapp.ui.theme.StartAppTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StartAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FirstScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}


@Composable
fun FirstScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Введите текст") },
            modifier = Modifier.fillMaxWidth()
        )
        /**
         * Кнопка открытия второй Activity
         */
        Button(
            onClick = {
                if (text.isBlank()) {
                    Toast.makeText(context, "Поле не заполнено!", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(context, SecondActivity::class.java).apply {
                        putExtra("message", text)
                    }
                    context.startActivity(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Открыть вторую Activity")
        }

        /**
         * Кнопка открытия звонилки
         */

        Button(
            onClick = {
                if (text.matches(Regex("^(\\+7|8)\\d{10}$"))) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = "tel:${text.trim()}".toUri()
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }

                } else {
                    Toast.makeText(context, "Введите номер телефона", Toast.LENGTH_SHORT).show()
                }

            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Позвонить другу")
        }
        /**
         * Кнопка Поделиться
         */
        Button(
            onClick = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"

                    putExtra(Intent.EXTRA_TEXT, text)
                }
                if (text.isNotBlank()) {
                    val chooser = Intent.createChooser(
                        sendIntent, "Поделиться через..."
                    )
                    context.startActivity(chooser)
                } else {
                    Toast.makeText(context, "Поле не заполнено", Toast.LENGTH_SHORT).show()
                }


            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Поделиться текстом")
        }
    }
}