package es.upm.pokemonapp.auth

import android.content.Context
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthHelper private constructor() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        @Volatile
        private var instance: FirebaseAuthHelper? = null

        fun getInstance(): FirebaseAuthHelper {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthHelper().also { instance = it }
            }
        }
    }

    fun getCurrentUser() = firebaseAuth.currentUser


    fun logout(context: Context, onLoggedOut: () -> Unit) {
        FirebaseAuth.getInstance().signOut()

        AuthUI.getInstance().signOut(context).addOnCompleteListener {
            onLoggedOut()
        }
    }
}
