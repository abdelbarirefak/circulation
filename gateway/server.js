const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const axios = require('axios');

const app = express();
app.use(cors());
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "http://localhost:5173",
        methods: ["GET", "POST"]
    }
});


let simulationPaused = false;
let simulationSpeed = 1.0;

// Bridge SSE stream from Java Backend to Socket.io
function connectToJavaSource() {
    console.log('Attempting to connect to Java SSE stream...');
    axios({
        method: 'get',
        url: 'http://localhost:8085/api/stream',
        responseType: 'stream'
    }).then(response => {
        console.log('Connected to Java SSE source');
        response.data.on('data', chunk => {
            const lines = chunk.toString().split('\n');
            lines.forEach(line => {
                if (line.startsWith('data: ')) {
                    try {
                        const data = JSON.parse(line.replace('data: ', ''));
                        io.emit('state_update', data);
                    } catch (e) {
                        // Incomplete or malformed JSON chunk
                    }
                }
            });
        });
        response.data.on('end', () => {
            console.warn('Java SSE stream ended. Reconnecting...');
            setTimeout(connectToJavaSource, 2000);
        });
    }).catch(err => {
        console.error('Failed to connect to Java Backend (is it running?). Retrying in 2s...');
        setTimeout(connectToJavaSource, 2000);
    });
}

connectToJavaSource();

// Proxy for Map Data
app.get('/api/map', async (req, res) => {
    try {
        console.log('Gateway: Fetching map from Java Backend (port 8085)...');
        const response = await axios.get('http://localhost:8085/api/map', { timeout: 5000 });
        console.log('Gateway: Successfully fetched map data.');
        res.json(response.data);
    } catch (err) {
        console.error('Gateway Error: Failed to fetch map from Java Backend:', err.message);
        if (err.response) {
            console.error('Java Backend responded with:', err.response.status, err.response.data);
        }
        res.status(500).json({
            error: 'Failed to fetch map from Java Backend',
            details: err.message
        });
    }
});

// Proxy for Control Actions (Legacy/Direct HTTP support if needed)
app.post('/api/control', async (req, res) => {
    try {
        await axios.post('http://localhost:8085/api/control', req.body);
        res.sendStatus(200);
    } catch (err) {
        res.sendStatus(500);
    }
});

io.on('connection', (socket) => {
    console.log('Client connected to Digital Twin Gateway');

    socket.on('control_action', (data) => {
        console.log('Control Action:', data);
        axios.post('http://localhost:8085/api/control', data).catch(e => { });

        if (data.type === 'pause') simulationPaused = true;
        if (data.type === 'resume') simulationPaused = false;
        if (data.type === 'speed') simulationSpeed = data.value;
    });

    socket.on('inject_incident', (data) => {
        console.log('Injecting Incident:', data);
        axios.post('http://localhost:8080/api/incident', data).catch(e => { });
    });
});

const PORT = 4000;
server.listen(PORT, () => {
    console.log(`Digital Twin Gateway running on port ${PORT}`);
});

