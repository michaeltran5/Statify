import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object NetworkUtils {

    private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    private const val API_URL = "https://api.spotify.com/v1/me"
    private const val CLIENT_ID = "your_client_id_here"
    private const val CLIENT_SECRET = "your_client_secret_here"
    private const val REDIRECT_URI = "your_custom_scheme://callback"

    suspend fun exchangeCodeForToken(code: String): String = withContext(Dispatchers.IO) {
        val url = URL(TOKEN_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        val postData = "grant_type=authorization_code&code=$code&redirect_uri=$REDIRECT_URI&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET"
        connection.outputStream.write(postData.toByteArray())

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(response)
        jsonObject.getString("access_token")
    }

    suspend fun fetchUserData(token: String): UserData = withContext(Dispatchers.IO) {
        val url = URL(API_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $token")

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(response)

        UserData(
            displayName = jsonObject.getString("display_name"),
            email = jsonObject.getString("email"),
            country = jsonObject.getString("country"),
            followers = jsonObject.getJSONObject("followers").getInt("total")
        )
    }
}