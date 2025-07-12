package com.bodakesatish.composefirebasefirestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class FirestoreClient {

    private val tag = "FirestoreClient: "

    private val db = FirebaseFirestore.getInstance()
    private val collection = "users"

    fun insertUser(
        user: User
    ): Flow<String?> {
        return callbackFlow {
            db.collection(collection)
                .add(user.toHashMap())
                .addOnSuccessListener { document ->
                    println(tag + "insert user with id: ${document.id}")

                    CoroutineScope(Dispatchers.IO).launch {
                        updateUser(user.copy(id = document.id)).collect {}
                    }

                    trySend(document.id)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    println(tag + "error inserting user: ${e.message}")
                    trySend(null)
                }

            awaitClose {}
        }
    }

    fun updateUser(
        user: User
    ): Flow<Boolean> {
        return callbackFlow {
            db.collection(collection)
                .document(user.id)
                .set(user.toHashMap())
                .addOnSuccessListener {
                    println(tag + "update user with id: ${user.id}")
                    trySend(true)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    println(tag + "error updating user: ${e.message}")
                    trySend(false)
                }

            awaitClose {}
        }
    }

    fun getUser(
        email: String
    ): Flow<User?> {
        return callbackFlow {
            db.collection(collection)
                .get()
                .addOnSuccessListener { result ->
                    var user: User? = null

                    for (document in result) {
                        if (document.data["email"] == email) {
                            user = document.data.toUser()
                            println(tag + "user found: ${user.email}")
                            trySend(user)
                        }
                    }

                    if (user == null) {
                        println(tag + "user not found: $email")
                        trySend(null)
                    }

                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    println(tag + "error getting user: ${e.message}")
                    trySend(null)
                }

            awaitClose {}
        }
    }

    private fun User.toHashMap(): HashMap<String, Any> {
        return hashMapOf(
            "id" to id,
            "name" to name,
            "email" to email,
            "age" to age
        )
    }

    private fun Map<String, Any>.toUser(): User {
        return User(
            id = this["id"] as String,
            name = this["name"] as String,
            email = this["email"] as String,
            age = (this["age"] as Long).toInt(),
        )
    }

}