package es.upm.pokemonapp.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import es.upm.pokemonapp.MainActivity
import es.upm.pokemonapp.utils.goToActivityWithoutReturn
import es.upm.pokemonapp.utils.showToast
import es.upm.pokemonapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.emailLoginButton.setOnClickListener {
            loginUser()
        }
    }

    private fun loginUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please fill in all fields")
            return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    goToActivityWithoutReturn(this, MainActivity::class.java)
                } else {
                    showToast("Error: ${task.exception?.localizedMessage}")
                }
            }
    }
}
