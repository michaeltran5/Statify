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
const REDIRECT_URI = process.env.REDIRECT_URI; // Make sure this is correct in your .env file

app.get('/login', (req, res) => {
    const scopes = 'user-read-private user-read-email';
    // Encode the redirect URI here:
    const redirectUriEncoded = encodeURIComponent(REDIRECT_URI);

    const authorizeURL = `https://accounts.spotify.com/authorize?response_type=code&client_id=${CLIENT_ID}&scope=${encodeURIComponent(scopes)}&redirect_uri=${redirectUriEncoded}`;
    res.redirect(authorizeURL);
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

        // Redirect to frontend with the access token
        res.redirect(`http://localhost:5500?access_token=${access_token}`);
    } catch (error) {
        res.status(400).send('Error during authentication');
    }
});

app.get('/user-data', async (req, res) => {
    const accessToken = req.headers.authorization?.split(' ')[1];

    if (!accessToken) {
        return res.status(401).send('No access token provided');
    }

    try {
        const response = await axios.get('https://api.spotify.com/v1/me', {
            headers: { 'Authorization': `Bearer ${accessToken}` }
        });
        res.json(response.data);
    } catch (error) {
        res.status(400).send('Error fetching user data');
    }
});

app.listen(port, () => {
    console.log(`Server running at http://localhost:${port}`);
});