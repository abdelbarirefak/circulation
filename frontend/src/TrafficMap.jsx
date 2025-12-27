import React, { useRef } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls, Text } from '@react-three/drei';
import * as THREE from 'three';

function Vehicle({ vehicle }) {
    const group = useRef();

    // Smooth Interpolation
    useFrame((state, delta) => {
        if (group.current) {
            group.current.position.x = THREE.MathUtils.lerp(group.current.position.x, vehicle.x, 0.15);
            group.current.position.y = THREE.MathUtils.lerp(group.current.position.y, -vehicle.y, 0.15);
        }
    });

    return (
        <group ref={group} position={[vehicle.x, -vehicle.y, 10]}>
            <mesh>
                <boxGeometry args={[18, 10, 6]} />
                <meshStandardMaterial color={vehicle.road.includes('RA') ? '#60a5fa' : '#f87171'} />
            </mesh>

            {/* Dynamic Thought Bubble */}
            <Text
                position={[0, 18, 0]}
                fontSize={7}
                color="white"
                font="https://fonts.gstatic.com/s/inter/v12/UcCOjFwsBxWZ8xyHeCOxkN72.woff"
                anchorX="center"
                anchorY="middle"
            >
                {vehicle.thought || "Cruising"}
            </Text>

            {/* Decision Ray (Forward direction) */}
            <mesh position={[12, 0, 0]}>
                <boxGeometry args={[4, 2, 2]} />
                <meshStandardMaterial color="white" emissive="white" emissiveIntensity={2} />
            </mesh>
        </group>
    );
}

function Road({ road }) {
    const length = Math.sqrt(Math.pow(road.endX - road.startX, 2) + Math.pow(road.endY - road.startY, 2));
    const angle = Math.atan2(road.endY - road.startY, road.endX - road.startX);
    const centerX = (road.startX + road.endX) / 2;
    const centerY = (road.startY + road.endY) / 2;

    return (
        <group position={[centerX, -centerY, 0]} rotation={[0, 0, -angle]}>
            {/* Road Surface */}
            <mesh>
                <planeGeometry args={[length, road.lanes * 28]} />
                <meshStandardMaterial color="#1e293b" />
            </mesh>

            {/* Lane Lines (Dashed) */}
            {[...Array(road.lanes - 1)].map((_, i) => (
                <mesh key={i} position={[0, (i - (road.lanes - 2) / 2) * 28, 0.1]}>
                    <planeGeometry args={[length, 1.5]} />
                    <meshBasicMaterial color="#475569" transparent opacity={0.5} />
                </mesh>
            ))}

            {/* Shoulder Glow */}
            <mesh position={[0, (road.lanes * 14), 0.1]}>
                <planeGeometry args={[length, 2]} />
                <meshBasicMaterial color="#3b82f6" />
            </mesh>
            <mesh position={[0, -(road.lanes * 14), 0.1]}>
                <planeGeometry args={[length, 2]} />
                <meshBasicMaterial color="#3b82f6" />
            </mesh>
        </group>
    );
}

function VehiclePath({ path }) {
    if (!path || path.length < 2) return null;
    const points = path.map(p => new THREE.Vector3(p[0], -p[1], 5));
    const curve = new THREE.CatmullRomCurve3(points);
    const pathPoints = curve.getPoints(50);

    return (
        <line>
            <bufferGeometry attach="geometry">
                <bufferAttribute
                    attach="attributes-position"
                    array={new Float32Array(pathPoints.flatMap(p => [p.x, p.y, p.z]))}
                    count={pathPoints.length}
                    itemSize={3}
                />
            </bufferGeometry>
            <lineBasicMaterial attach="material" color="#3b82f6" transparent opacity={0.3} linewidth={2} />
        </line>
    );
}

export function TrafficMap({ state, map }) {
    return (
        <Canvas camera={{ position: [500, -500, 1000], fov: 45 }}>
            <ambientLight intensity={0.5} />
            <pointLight position={[1000, 1000, 1000]} />

            {/* Roads */}
            {map.roads.map(r => (
                <Road key={r.id} road={r} />
            ))}

            {/* Vehicle Paths (Transparency Layer) */}
            {state.vehicles.map(v => (
                <VehiclePath key={`path-${v.id}`} path={v.path} />
            ))}

            {/* Vehicles */}
            {state.vehicles.map(v => (
                <Vehicle key={v.id} vehicle={v} />
            ))}

            <OrbitControls enableRotate={false} panSpeed={2} />
            <gridHelper args={[2000, 50, "#1e293b", "#0f172a"]} rotation={[Math.PI / 2, 0, 0]} />
        </Canvas>
    );
}
