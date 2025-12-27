import { useState, useEffect } from 'react';

export function useTrafficStream() {
    const [state, setState] = useState({ vehicles: [] });
    const [map, setMap] = useState({ roads: [] });

    useEffect(() => {
        // 1. Fetch static map
        fetch('http://localhost:8080/api/map')
            .then(res => res.json())
            .then(data => setMap(data))
            .catch(err => console.error('Map fetch failed:', err));

        // 2. Listen to real-time stream
        const eventSource = new EventSource('http://localhost:8080/api/stream');

        eventSource.onmessage = (event) => {
            const newState = JSON.parse(event.data);
            setState(newState);
        };

        return () => eventSource.close();
    }, []);

    return { state, map };
}
