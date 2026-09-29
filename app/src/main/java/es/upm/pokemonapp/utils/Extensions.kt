
package es.upm.pokemonapp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide


fun Context.showToast(message: String, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, message, duration).show()
}

fun ImageView.loadFromUrl(url: String?) {
    if (url.isNullOrBlank()) {
        setImageResource(es.upm.pokemonapp.R.mipmap.image_not_found)
        return
    }

    val context = this.context
    if (context is Activity && (context.isDestroyed || context.isFinishing)) {
        Log.w("ImageViewLoad", "Activity is destroyed or finishing, skipping image load.")
        return
    }

    Glide.with(this)
        .load(url.trim())
        .placeholder(es.upm.pokemonapp.R.mipmap.image_not_found)
        .error(es.upm.pokemonapp.R.mipmap.image_not_found)
        .into(this)
}

fun goToActivityWithoutReturn(fromActivity: Activity, destination: Class<out Activity>) {
    val intent = Intent(fromActivity, destination)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    fromActivity.startActivity(intent)
    fromActivity.finish()
}


