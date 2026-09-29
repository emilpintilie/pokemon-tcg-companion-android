package es.upm.pokemonapp.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import es.upm.pokemonapp.databinding.ActivityRecoverPasswordBinding
import es.upm.pokemonapp.utils.goToActivityWithoutReturn
import es.upm.pokemonapp.utils.showToast

class RecoverPasswordActivityCustom : AppCompatActivity(){
    private lateinit var binding: ActivityRecoverPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoverPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.sendRecoverPasswordMailButton.setOnClickListener {
            sendRecoverPasswordMail()
        }
    }

    private fun sendRecoverPasswordMail() {
        val email = binding.emailEditText.text.toString().trim()
        if (email.isEmpty()) {
            showToast("Please fill in all fields")
            return
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Recovery mail sent")
                    goToActivityWithoutReturn(this, AuthActivity::class.java)
                } else {
                    showToast("Error: ${task.exception?.localizedMessage}")
                }
            }
    }
}