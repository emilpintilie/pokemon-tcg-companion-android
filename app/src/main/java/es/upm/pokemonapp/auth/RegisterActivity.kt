package es.upm.pokemonapp.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import es.upm.pokemonapp.MainActivity
import es.upm.pokemonapp.databinding.ActivityRegisterBinding
import es.upm.pokemonapp.utils.goToActivityWithoutReturn
import es.upm.pokemonapp.utils.showToast

class RegisterActivity : AppCompatActivity(){
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.emailRegisterButton.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val displayName = binding.userNameText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.passwordConfirmEditText.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please fill in all fields")
            return
        }else if ((password != confirmPassword)){
            showToast("The passwords doesn't match")
            return
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    setUserName(displayName)
                    goToActivityWithoutReturn(this, MainActivity::class.java)
                } else {
                    showToast("Error: ${task.exception?.localizedMessage}")
                }
            }
    }
    private fun setUserName(displayName: String){
        //Set the user name
        val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName) // Establecer el nombre
                .build()

            user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("User created successfully")
                } else {
                    showToast("Error: ${task.exception?.localizedMessage}")
                }
            }
        }
    }
}