# Statify - Spotify User Data Analytics

Statify is a web application that leverages the Spotify API to provide users with insights into their listening habits. It integrates with Firebase for data storage and user management.

## Features

- User authentication with Spotify
- Display user's top 10 tracks
- Display user's top 10 artists
- Show user's top 5 genres
- Calculate total listening time over the past year
- Store user's top tracks in Firebase for persistence

## Technologies Used

- Node.js
- Express.js
- Spotify Web API
- Firebase (Firestore)
- Axios for HTTP requests
- dotenv for environment variable management

## Prerequisites

Before you begin, ensure you have met the following requirements:

- Node.js installed (version 14.x or higher)
- A Spotify Developer account and registered application
- A Firebase project set up with Firestore enabled

## Setup and Installation

1. Clone the repository:
   ```
   git clone https://github.com/your-username/statify.git
   cd statify
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Set up environment variables:
   Create a `.env` file in the root directory and add the following:
   ```
   SPOTIFY_CLIENT_ID=your_spotify_client_id
   SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
   REDIRECT_URI=http://localhost:3000/callback
   GOOGLE_APPLICATION_CREDENTIALS=./.config/your-firebase-adminsdk-file.json
   ```

4. Set up Firebase:
   - Create a Firebase project in the Firebase Console
   - Generate a new private key for your service account
   - Save the JSON file in the `.config` folder of your project
   - Ensure the path in `GOOGLE_APPLICATION_CREDENTIALS` matches your file's location

5. Run the server:
   ```
   node server.js
   ```

## Usage

1. Navigate to `http://localhost:3000/login` in your web browser
2. Log in with your Spotify account
3. Once authenticated, you'll be redirected to the main application page
4. Explore your Spotify data and analytics!

## API Endpoints

- `/login`: Initiates Spotify authentication
- `/callback`: Handles Spotify authentication callback
- `/user-data`: Fetches user profile information
- `/top-tracks`: Retrieves user's top 10 tracks
- `/top-artists`: Retrieves user's top 10 artists
- `/listening-time`: Calculates total listening time over the past year
- `/top-genres`: Fetches user's top 5 genres

## Firebase Integration

This project uses Firebase Firestore to store user data. When a user logs in, their top tracks are stored in the Firestore database under their Spotify user ID. This allows for data persistence and potential future features like trend analysis over time.

## Contributing

Contributions to Statify are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgements

- Spotify Web API
- Firebase
- Express.js community
