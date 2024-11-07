const express = require('express');
const axios = require('axios');
const cors = require('cors');
const admin = require('firebase-admin');
const path = require('path');
require('dotenv').config();

const app = express();
const port = 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Environment variables
const CLIENT_ID = process.env.SPOTIFY_CLIENT_ID;
const CLIENT_SECRET = process.env.SPOTIFY_CLIENT_SECRET;
const REDIRECT_URI = process.env.REDIRECT_URI || 'http://localhost:3000/callback';

// Initialize Firebase
const serviceAccount = require(path.join(__dirname, process.env.GOOGLE_APPLICATION_CREDENTIALS));
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: "statify-d0d26" // Your Firebase project ID
});
const db = admin.firestore();

// Spotify API helpers
const getSpotifyApi = (accessToken) => axios.create({
    baseURL: 'https://api.spotify.com/v1',
    headers: { 'Authorization': `Bearer ${accessToken}` }
});

const getAccessToken = async (code) => {
    const response = await axios({
        method: 'post',
        url: 'https://accounts.spotify.com/api/token',
        params: {
            grant_type: 'authorization_code',
            code: code,
            redirect_uri: REDIRECT_URI,
        },
        headers: {
            'Authorization': 'Basic ' + Buffer.from(CLIENT_ID + ':' + CLIENT_SECRET).toString('base64'),
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    });
    return response.data.access_token;
};

// Route handlers
app.get('/login', (req, res) => {
    const scopes = 'user-read-private user-read-email user-top-read user-read-recently-played';
    res.redirect(`https://accounts.spotify.com/authorize?response_type=code&client_id=${CLIENT_ID}&scope=${encodeURIComponent(scopes)}&redirect_uri=${encodeURIComponent(REDIRECT_URI)}`);
});

app.get('/callback', async (req, res) => {
    const code = req.query.code;
    try {
        const accessToken = await getAccessToken(code);
        console.log('Access token obtained');

        const spotifyApi = getSpotifyApi(accessToken);

        // Get user data
        const userDataResponse = await spotifyApi.get('/me');
        const userId = userDataResponse.data.id;

        // Get top tracks
        const topTracksResponse = await spotifyApi.get('/me/top/tracks', { params: { limit: 10 } });
        const topTracks = topTracksResponse.data.items.map(track => ({
            id: track.id,
            name: track.name,
            artist: track.artists[0].name,
            album: track.album.name
        }));

        // Store in Firebase
        await db.collection('users').doc(userId).set({
            topTracks: topTracks,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        console.log('Top tracks uploaded to Firebase');

        res.redirect(`http://localhost:5500?access_token=${accessToken}`);
    } catch (error) {
        console.error('Error during authentication:', error.response?.data || error.message);
        res.status(500).send('Authentication failed');
    }
});

app.get('/user-data', async (req, res) => {
    const accessToken = req.headers.authorization?.split(' ')[1];
    if (!accessToken) {
        return res.status(401).json({ error: 'No access token provided' });
    }

    try {
        const spotifyApi = getSpotifyApi(accessToken);
        const response = await spotifyApi.get('/me');
        res.json(response.data);
    } catch (error) {
        console.error('Error fetching user data:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json({
            error: 'Error fetching user data',
            details: error.response?.data || error.message
        });
    }
});

app.get('/top-tracks', async (req, res) => {
    const accessToken = req.headers.authorization?.split(' ')[1];
    if (!accessToken) {
        return res.status(401).json({ error: 'No access token provided' });
    }

    try {
        const spotifyApi = getSpotifyApi(accessToken);
        const response = await spotifyApi.get('/me/top/tracks', { params: { limit: 10 } });
        res.json(response.data.items);
    } catch (error) {
        console.error('Error fetching top tracks:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json({
            error: 'Error fetching top tracks',
            details: error.response?.data || error.message
        });
    }
});

app.get('/top-artists', async (req, res) => {
    const accessToken = req.headers.authorization?.split(' ')[1];
    if (!accessToken) {
        return res.status(401).json({ error: 'No access token provided' });
    }

    try {
        const spotifyApi = getSpotifyApi(accessToken);
        const response = await spotifyApi.get('/me/top/artists', { params: { limit: 10 } });
        res.json(response.data.items);
    } catch (error) {
        console.error('Error fetching top artists:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json({
            error: 'Error fetching top artists',
            details: error.response?.data || error.message
        });
    }
});

app.get('/listening-time', async (req, res) => {
    const accessToken = req.headers.authorization?.split(' ')[1];
    if (!accessToken) {
        return res.status(401).json({ error: 'No access token provided' });
    }

    try {
        const spotifyApi = getSpotifyApi(accessToken);
        const now = new Date();
        const oneYearAgo = new Date(now.getFullYear() - 1, now.getMonth(), now.getDate());

        let totalDurationMs = 0;
        let next = `/me/player/recently-played?limit=50&before=${now.getTime()}`;

        while (next) {
            const response = await spotifyApi.get(next);

            for (let item of response.data.items) {
                const playedAt = new Date(item.played_at);
                if (playedAt > oneYearAgo) {
                    totalDurationMs += item.track.duration_ms;
                } else {
                    next = null;
                    break;
                }
            }

            next = response.data.next;
        }

        const totalMinutes = Math.round(totalDurationMs / 60000);
        res.json({ totalMinutes });
    } catch (error) {
        console.error('Error calculating listening time:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json({
            error: 'Error calculating listening time',
            details: error.response?.data || error.message
        });
    }
});

app.get('/top-genres', async (req, res) => {
    const accessToken = req.headers.authorization?.split(' ')[1];
    if (!accessToken) {
        return res.status(401).json({ error: 'No access token provided' });
    }

    try {
        const spotifyApi = getSpotifyApi(accessToken);
        const response = await spotifyApi.get('/me/top/artists', { params: { limit: 50 } });

        const genreCounts = {};
        response.data.items.forEach(artist => {
            artist.genres.forEach(genre => {
                genreCounts[genre] = (genreCounts[genre] || 0) + 1;
            });
        });

        const sortedGenres = Object.entries(genreCounts)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 5)
            .map(([genre, count]) => ({ genre, count }));

        res.json(sortedGenres);
    } catch (error) {
        console.error('Error fetching top genres:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json({
            error: 'Error fetching top genres',
            details: error.response?.data || error.message
        });
    }
});

app.listen(port, () => {
    console.log(`Server running at http://localhost:${port}`);
});