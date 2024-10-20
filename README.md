# Spotify Top Tracks Viewer

This project allows users to view their top 10 most listened to tracks on Spotify.

## Setup Instructions

1. Clone the repository:
   ```
   git clone [your_repository_url]
   cd [repository_name]
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Set up environment variables:
   - Create a file named `.env` in the project root directory
   - Add the following content:
     ```
     SPOTIFY_CLIENT_ID=your_client_id_here
     SPOTIFY_CLIENT_SECRET=your_client_secret_here
     REDIRECT_URI=http://localhost:3000/callback
     ```

4. Set up Spotify Developer account:
   - Create a Spotify Developer account at https://developer.spotify.com/
   - Create a new application in the Spotify Developer Dashboard
   - Set the Redirect URI to `http://localhost:3000/callback` in the app settings
   - Copy the Client ID and Client Secret to use in the `.env` file

5. Start the server:
   ```
   node server.js
   ```

6. In a separate terminal, serve the HTML file:
   - If using Python:
     ```
     python -m http.server 5500
     ```
   - If using Node.js:
     ```
     npx http-server -p 5500
     ```

7. Open a web browser and navigate to `http://localhost:5500`

## Note

Do not commit the `.env` file or share your Spotify API credentials with others.

## Troubleshooting

If you encounter any issues, please check the following:
- Ensure all dependencies are installed
- Verify that your Spotify API credentials are correct
- Check that the server is running on port 3000 and the HTML is being served on port 5500

For any other problems, please open an issue in the GitHub repository.
