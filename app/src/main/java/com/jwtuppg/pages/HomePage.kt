package com.jwtuppg.pages

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jwtuppg.AuthState
import com.jwtuppg.AuthViewModel
import com.jwtuppg.Messages.Message

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val messages = remember { mutableStateListOf<Message>() }
    val authState by authViewModel.authState.observeAsState()
    val listState = rememberLazyListState() // Skapa ett tillstånd för LazyList

    // Lyssna på meddelanden i realtid
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            fetchMessages(messages, (authState as AuthState.Authenticated).token)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Home Page", fontSize = 32.sp)

        // Lista meddelanden med LazyColumn och scrollning
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState // Använd listan tillstånd
        ) {
            items(messages) { message ->
                Text(
                    text = "${message.user}: ${message.content}",
                    modifier = Modifier.padding(8.dp),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Skicka nytt meddelande
        var messageText by remember { mutableStateOf("") }

        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text("Write a message") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = {
            val currentUser = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"
            val token = (authState as? AuthState.Authenticated)?.token
            val message = Message(
                user = currentUser,
                content = messageText,
                timestamp = System.currentTimeMillis()
            )
            saveMessage(message, token) {
                messageText = "" // Töm textfältet efter att meddelandet har skickats
            }
        }) {
            Text(text = "Send")
        }

        TextButton(onClick = {
            authViewModel.signout()
            navController.navigate("login")
        }) {
            Text(text = "Sign Out")
        }
    }
}

fun saveMessage(message: Message, jwtToken: String?, onSuccess: () -> Unit) {
    if (jwtToken == null) {
        Log.e("HomePage", "Missing JWT Token")
        return
    }

    val db = FirebaseFirestore.getInstance()
    db.collection("messages")
        .add(message.copy(id = ""))
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e("HomePage", "Error saving message: ", e)
        }
}

fun fetchMessages(messages: SnapshotStateList<Message>, jwtToken: String?) {
    val db = FirebaseFirestore.getInstance()

    // Använd addSnapshotListener för att få realtidsuppdateringar
    db.collection("messages")
        .orderBy("timestamp")
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("HomePage", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                messages.clear()
                for (document in snapshot.documents) {
                    val message = document.toObject(Message::class.java)
                    message?.let { messages.add(it) }
                }
            }
        }
}