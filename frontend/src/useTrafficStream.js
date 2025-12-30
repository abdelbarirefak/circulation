import { useState, useEffect } from 'react';
import { io } from 'socket.io-client';

export function useTrafficStream() {
    const [state, setState] = useState({ vehicles: [] });
    const [map, setMap] = useState({ roads: [] });

    useEffect(() => {
        const fetchMap = () => {
            fetch('/api/map')
                .then(res => {
                    if (!res.ok) throw new Error('Gateway failed to fetch map');
                    return res.json();
                })
                .then(data => {
                    if (data && data.roads && data.roads.length > 0) {
                        setMap(data);
                        console.log('Map loaded successfully:', data.roads.length, 'roads');
                    }
                })
                .catch(err => console.error('Map fetch failed:', err));
        };

        // 1. Initial fetch
        fetchMap();

        // 2. Connect to Digital Twin Gateway
        const socket = io();

        socket.on('connect', () => {
            console.log('Connected to Gateway, refetching map...');
            fetchMap();
        });

        socket.on('state_update', (newState) => {
            setState(newState);
        });

        return () => socket.disconnect();
    }, []);

    return { state, map };
}
