import React, { useRef } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls, Text } from '@react-three/drei';
import * as THREE from 'three';

function Vehicle({ vehicle, isSelected, onClick }) {
    const group = useRef();

    useFrame((state, delta) => {
        if (group.current) {
            group.current.position.x = THREE.MathUtils.lerp(group.current.position.x, vehicle.x, 0.15);
            group.current.position.y = THREE.MathUtils.lerp(group.current.position.y, -vehicle.y, 0.15);
            group.current.position.z = THREE.MathUtils.lerp(group.current.position.z, (vehicle.z || 0) + 10, 0.15);
        }
    });

    return (
        <group ref={group} onClick={(e) => { e.stopPropagation(); onClick(); }}>
            <mesh>
                <boxGeometry args={[18, 10, 6]} />
                <meshStandardMaterial
                    color={isSelected ? '#fbbf24' : (vehicle.road.includes('RA') ? '#60a5fa' : '#f87171')}
                    emissive={isSelected ? '#fbbf24' : '#000000'}
                    emissiveIntensity={isSelected ? 0.5 : 0}
                />
            </mesh>

            {/* Selection Ring */}
            {isSelected && (
                <mesh position={[0, 0, -3]}>
                    <ringGeometry args={[12, 14, 32]} />
                    <meshBasicMaterial color="#fbbf24" transparent opacity={0.6} side={THREE.DoubleSide} />
                </mesh>
            )}

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

            <mesh position={[12, 0, 0]}>
                <boxGeometry args={[4, 2, 2]} />
                <meshStandardMaterial color="white" emissive="white" emissiveIntensity={2} />
            </mesh>
        </group>
    );
}

function Road({ road }) {
    const startX = road.startX || 0;
    const startY = road.startY || 0;
    const endX = road.endX || 0;
    const endY = road.endY || 0;
    const lanes = road.lanes || 2;

    const length = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)) || 1;
    const angle = Math.atan2(endY - startY, endX - startX);
    const centerX = (startX + endX) / 2;
    const centerY = (startY + endY) / 2;

    return (
        <group position={[centerX, -centerY, 0]} rotation={[0, 0, -angle]}>
            {/* Road Surface */}
            <mesh>
                <planeGeometry args={[length, lanes * 28]} />
                <meshStandardMaterial
                    color="#ffffff"
                    emissive="#ffffff"
                    emissiveIntensity={0.5}
                />
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

export function TrafficMap({ state, map, onSelectVehicle, selectedId }) {
    console.log('DIAGNOSTIC: TrafficMap Rendering', { roads: map?.roads?.length, vehicles: state?.vehicles?.length });

    return (
        <Canvas
            camera={{ position: [400, -400, 2000], fov: 50, near: 1, far: 10000 }}
            style={{ background: '#020617' }}
        >
            <ambientLight intensity={3} />

            {/* LARGE CENTER MARKER */}
            <mesh position={[400, -400, 0]}>
                <sphereGeometry args={[50, 32, 32]} />
                <meshStandardMaterial color="white" emissive="white" emissiveIntensity={5} />
            </mesh>

            <mesh position={[0, 0, 0]}>
                <sphereGeometry args={[30, 32, 32]} />
                <meshStandardMaterial color="red" emissive="red" emissiveIntensity={5} />
            </mesh>

            {/* Roads */}
            {map?.roads?.map(r => (
                <Road key={r.id} road={r} />
            ))}

            {/* Vehicles */}
            {state?.vehicles?.map(v => (
                <Vehicle
                    key={v.id}
                    vehicle={v}
                    isSelected={v.id === selectedId}
                    onClick={() => onSelectVehicle(v)}
                />
            ))}

            <OrbitControls
                target={[400, -400, 0]}
            />
            <gridHelper args={[5000, 50]} rotation={[Math.PI / 2, 0, 0]} />
        </Canvas>
    );
}
