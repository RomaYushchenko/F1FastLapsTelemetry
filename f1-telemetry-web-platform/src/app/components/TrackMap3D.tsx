import { Canvas } from '@react-three/fiber'
import { Html, Line, OrbitControls } from '@react-three/drei'
import { useMemo } from 'react'
import type { CarPositionDto, TrackLayoutResponseDto, TrackPoint3D } from '@/api/types'
import {
  SECTOR_COLORS,
  computeBounds,
  computeThreeTransform,
  splitIntoSectors,
  worldToThree,
} from '@/utils/trackNormalization'

interface Props {
  layout: TrackLayoutResponseDto
  cars: CarPositionDto[]
}

export function TrackMap3D({ layout, cars }: Props) {
  const bounds = layout.bounds ?? computeBounds(layout.points)
  const transform = useMemo(() => computeThreeTransform(bounds), [bounds])
  const sectors = layout.sectorBoundaries ?? []

  const [s1pts, s2pts, s3pts] = splitIntoSectors(layout.points, sectors)

  const toThreePoints = (pts: TrackPoint3D[]) =>
    pts.map(p => worldToThree(p.x, p.y, p.z, transform))

  const elevRange = bounds.maxElev - bounds.minElev

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <Canvas camera={{ position: [0, 80, 120], fov: 50 }}>
        <ambientLight intensity={0.5} />
        <directionalLight position={[50, 100, 50]} intensity={0.8} />

        {s1pts.length > 1 && (
          <Line points={toThreePoints(s1pts)} color={SECTOR_COLORS[1]} lineWidth={4} />
        )}
        {s2pts.length > 1 && (
          <Line points={toThreePoints(s2pts)} color={SECTOR_COLORS[2]} lineWidth={4} />
        )}
        {s3pts.length > 1 && (
          <Line points={toThreePoints(s3pts)} color={SECTOR_COLORS[3]} lineWidth={4} />
        )}

        {sectors
          .filter(b => b.sector !== 1)
          .map(b => {
            const pos = worldToThree(b.x, b.y, b.z, transform)
            const color = SECTOR_COLORS[b.sector as 2 | 3]
            return (
              <group key={`sector-${b.sector}`} position={pos}>
                <mesh>
                  <sphereGeometry args={[2, 12, 12]} />
                  <meshStandardMaterial color={color} emissive={color} emissiveIntensity={0.6} />
                </mesh>
                <Html position={[0, 4, 0]} center>
                  <span
                    style={{
                      color,
                      fontSize: 12,
                      fontWeight: 'bold',
                      background: 'rgba(0,0,0,0.6)',
                      padding: '1px 4px',
                      borderRadius: 3,
                    }}
                  >
                    S{b.sector}
                  </span>
                </Html>
              </group>
            )
          })}

        {sectors.find(b => b.sector === 1) && (() => {
          const s1 = sectors.find(b => b.sector === 1)!
          const pos = worldToThree(s1.x, s1.y, s1.z, transform)
          return (
            <group position={pos}>
              <mesh>
                <boxGeometry args={[1, 4, 0.5]} />
                <meshStandardMaterial color="#FFFFFF" />
              </mesh>
              <Html position={[0, 6, 0]} center>
                <span
                  style={{
                    color: '#FFFFFF',
                    fontSize: 11,
                    fontWeight: 'bold',
                    background: 'rgba(0,0,0,0.6)',
                    padding: '1px 4px',
                    borderRadius: 3,
                  }}
                >
                  S/F
                </span>
              </Html>
            </group>
          )
        })()}

        {cars.map(car => {
          const pos = worldToThree(
            car.worldPosX,
            car.worldPosY ?? bounds.minElev,
            car.worldPosZ,
            transform,
          )
          return (
            <mesh key={car.carIndex} position={pos}>
              <sphereGeometry args={[1.5, 16, 16]} />
              <meshStandardMaterial
                color={car.color}
                emissive={car.color}
                emissiveIntensity={0.4}
              />
            </mesh>
          )
        })}

        <gridHelper args={[120, 30, '#1F2937', '#1F2937']} />

        <OrbitControls
          enablePan
          enableZoom
          enableRotate
          minDistance={15}
          maxDistance={250}
        />
      </Canvas>

      {elevRange > 0.5 && (
        <p className="text-xs text-text-secondary mt-1 text-center">
          Elevation change: {elevRange.toFixed(1)} m
        </p>
      )}
    </div>
  )
}

