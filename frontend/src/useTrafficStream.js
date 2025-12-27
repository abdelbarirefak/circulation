import { useState, useEffect } from 'react';
import { io } from 'socket.io-client';

export function useTrafficStream() {
    const [state, setState] = useState({ vehicles: [] });
    const [map, setMap] = useState({ roads: [] });

    useEffect(() => {
        // 1. Fetch static map
        fetch('http://localhost:8080/api/map')
            .then(res => res.json())
            .then(data => setMap(data))
            .catch(err => console.error('Map fetch failed:', err));

        // 2. Connect to Digital Twin Gateway
        const socket = io('http://localhost:3001');

        socket.on('state_update', (newState) => {
            setState(newState);
        });

        return () => socket.disconnect();
    }, []);

    return { state, map };
}
