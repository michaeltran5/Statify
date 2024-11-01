import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.statify.R
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var loginButton: Button
    private lateinit var userDataTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        loginButton.setOnClickListener {
            initiateSpotifyLogin()
        }

        // Check if we're returning from Spotify auth
        val uri = intent.data
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                exchangeCodeForToken(code)
            }
        }
    }

    private fun initiateSpotifyLogin() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
            "${AUTH_URL}?client_id=${CLIENT_ID}&response_type=code&redirect_uri=${REDIRECT_URI}&scope=user-read-private%20user-read-email"
        ))
        startActivity(intent)
    }

    private fun exchangeCodeForToken(code: String) {
        lifecycleScope.launch {
            try {
                val token = NetworkUtils.exchangeCodeForToken(code)
                fetchUserData(token)
            } catch (e: Exception) {
                userDataTextView.text = "Error: ${e.message}"
            }
        }
    }

    private fun fetchUserData(token: String) {
        lifecycleScope.launch {
            try {
                val userData = NetworkUtils.fetchUserData(token)
                displayUserData(userData)
            } catch (e: Exception) {
                userDataTextView.text = "Error: ${e.message}"
            }
        }
    }

    private fun displayUserData(userData: UserData) {
        userDataTextView.text = """
            Name: ${userData.displayName}
            Email: ${userData.email}
            Country: ${userData.country}
            Followers: ${userData.followers}
        """.trimIndent()
    }

    companion object {
        private const val CLIENT_ID = "your_client_id_here"
        private const val REDIRECT_URI = "your_custom_scheme://callback"
        private const val AUTH_URL = "https://accounts.spotify.com/authorize"
    }
}