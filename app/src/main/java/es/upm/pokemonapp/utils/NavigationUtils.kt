// NavigationExtensions.kt
package es.upm.pokemonapp.utils

import android.content.Intent
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import es.upm.pokemonapp.MainActivity
import es.upm.pokemonapp.OpenStreetMapActivity
import es.upm.pokemonapp.DeckActivity
import es.upm.pokemonapp.R
import es.upm.pokemonapp.RecommendationsActivity
import es.upm.pokemonapp.SettingsActivity
import es.upm.pokemonapp.SpeciesActivity

object NavigationUtils {

    fun navViewClick(
        item: MenuItem,
        currentNavigationId: Int,
        context: AppCompatActivity
    ): Boolean {
        return when (item.itemId) {
            R.id.navigation_home -> handleNavigation(
                context,
                item,
                currentNavigationId,
                MainActivity::class.java,
                "Already in Home"
            )
            R.id.navigation_map -> handleNavigation(
                context,
                item,
                currentNavigationId,
                OpenStreetMapActivity::class.java,
                "Already in Map"
            )
            R.id.navigation_deck -> handleNavigation(
                context,
                item,
                currentNavigationId,
                DeckActivity::class.java,
                "Already in Deck"
            )
            R.id.navigation_list -> handleNavigation(
                context,
                item,
                currentNavigationId,
                SpeciesActivity::class.java,
                "Already in Species List"
            )
            R.id.navigation_settings -> handleNavigation(
                context,
                item,
                currentNavigationId,
                SettingsActivity::class.java,
                "Already in Settings"
            )
            R.id.navigation_recommendations -> handleNavigation(
                context,
                item,
                currentNavigationId,
                RecommendationsActivity::class.java,
                "Already in Recommendations"
            )
            else -> false
        }
    }

    private fun handleNavigation(
        context: AppCompatActivity,
        item: MenuItem,
        currentId: Int,
        destination: Class<out AppCompatActivity>,
        message: String
    ): Boolean {
        return if (item.itemId == currentId) {
            context.showToast(message)
            true
        } else {
            navigateTo(context, destination)
            true
        }
    }

    private fun navigateTo(context: AppCompatActivity, destination: Class<out AppCompatActivity>) {
        if (context::class.java != destination) {
            Intent(context, destination).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(this)
                context.overridePendingTransition(0, 0)
            }
        }
    }
}
