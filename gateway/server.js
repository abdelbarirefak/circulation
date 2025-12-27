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
        origin: "*",
        methods: ["GET", "POST"]
    }
});

let simulationPaused = false;
let simulationSpeed = 1.0;

// Poll Java Backend for state (Bridge until active JADE Gateway is used)
setInterval(async () => {
    try {
        const response = await axios.get('http://localhost:8080/api/stream');
        // Java SSE stream is usually handled differently, but for simplicity here 
        // we assume a consolidated state endpoint exists or we use the SSE stream directly.
        // For the Digital Twin, we'll emit the periodic state.
        io.emit('state_update', response.data);
    } catch (err) {
        // Java backend might be down
    }
}, 50); // 20Hz polling for smooth UI

io.on('connection', (socket) => {
    console.log('Client connected to Digital Twin Gateway');

    socket.on('control_action', (data) => {
        console.log('Control Action:', data);
        // Forward to Java Backend
        axios.post('http://localhost:8080/api/control', data).catch(e => { });

        if (data.type === 'pause') simulationPaused = true;
        if (data.type === 'resume') simulationPaused = false;
        if (data.type === 'speed') simulationSpeed = data.value;
    });

    socket.on('inject_incident', (data) => {
        console.log('Injecting Incident:', data);
        axios.post('http://localhost:8080/api/incident', data).catch(e => { });
    });
});

const PORT = 3001;
server.listen(PORT, () => {
    console.log(`Digital Twin Gateway running on port ${PORT}`);
});
