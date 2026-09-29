package es.upm.pokemonapp.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import es.upm.pokemonapp.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        //Login button
        binding.loginButton.setOnClickListener { loginUser() }
        //Register button
        binding.registerButton.setOnClickListener { registerUser() }
        //Recover Password button
        binding.recoverPasswordButton.setOnClickListener { recoverPassword() }
    }

    private fun loginUser() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun registerUser(){
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun recoverPassword(){
        val intent = Intent(this, RecoverPasswordActivityCustom::class.java)
        startActivity(intent)
    }
}
