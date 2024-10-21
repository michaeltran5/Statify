const express = require('express');
const axios = require('axios');
const cors = require('cors');
require('dotenv').config();

const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());

const CLIENT_ID = process.env.SPOTIFY_CLIENT_ID;
const CLIENT_SECRET = process.env.SPOTIFY_CLIENT_SECRET;
const REDIRECT_URI = process.env.REDIRECT_URI || 'http://localhost:3000/callback';

app.get('/login', (req, res) => {
    const scopes = 'user-read-private user-read-email user-top-read user-read-recently-played';
    res.redirect(`https://accounts.spotify.com/authorize?response_type=code&client_id=${CLIENT_ID}&scope=${encodeURIComponent(scopes)}&redirect_uri=${encodeURIComponent(REDIRECT_URI)}`);
});

app.get('/callback', async (req, res) => {
    const code = req.query.code;
    try {
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

        const { access_token } = response.data;
        console.log('Access token obtained');

        res.redirect(`http://localhost:5500?access_token=${access_token}`);
    } catch (error) {
        console.error('Error during authentication:', error.response?.data || error.message);
        res.status(500).send('Authentication failed');
    }
});

app.get('/user-data', async (req, res) => {
    console.log('Received request for /user-data');
    const accessToken = req.headers.authorization?.split(' ')[1];

    if (!accessToken) {
        console.log('No access token provided for /user-data');
        return res.status(401).json({ error: 'No access token provided' });
    }

    try {
        const response = await axios.get('https://api.spotify.com/v1/me', {
            headers: { 'Authorization': `Bearer ${accessToken}` }
        });
        console.log('User data fetched successfully');
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
    console.log('Received request for /top-tracks');
    const accessToken = req.headers.authorization?.split(' ')[1];

    if (!accessToken) {
        console.log('No access token provided for /top-tracks');
        return res.status(401).json({ error: 'No access token provided' });
    }

    try {
        const response = await axios.get('https://api.spotify.com/v1/me/top/tracks', {
            headers: { 'Authorization': `Bearer ${accessToken}` },
            params: { limit: 10 }
        });
        console.log('Top tracks fetched successfully');
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
    console.log('Received request for /top-artists');
    const accessToken = req.headers.authorization?.split(' ')[1];

    if (!accessToken) {
        console.log('No access token provided for /top-artists');
        return res.status(401).json({ error: 'No access token provided' });
    }

    try {
        const response = await axios.get('https://api.spotify.com/v1/me/top/artists', {
            headers: { 'Authorization': `Bearer ${accessToken}` },
            params: { limit: 10 }
        });
        console.log('Top artists fetched successfully');
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
    console.log('Received request for /listening-time');
    const accessToken = req.headers.authorization?.split(' ')[1];

    if (!accessToken) {
        console.log('No access token provided for /listening-time');
        return res.status(401).json({ error: 'No access token provided' });
    }

    try {
        const now = new Date();
        const oneYearAgo = new Date(now.getFullYear() - 1, now.getMonth(), now.getDate());

        let totalDurationMs = 0;
        let next = `https://api.spotify.com/v1/me/player/recently-played?limit=50&before=${now.getTime()}`;

        while (next) {
            const response = await axios.get(next, {
                headers: { 'Authorization': `Bearer ${accessToken}` }
            });

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
        console.log('Total listening time calculated successfully');
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
    console.log('Received request for /top-genres');
    const accessToken = req.headers.authorization?.split(' ')[1];

    if (!accessToken) {
        console.log('No access token provided for /top-genres');
        return res.status(401).json({ error: 'No access token provided' });
    }

    try {
        const response = await axios.get('https://api.spotify.com/v1/me/top/artists', {
            headers: { 'Authorization': `Bearer ${accessToken}` },
            params: { limit: 50 }
        });

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

        console.log('Top genres calculated successfully');
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