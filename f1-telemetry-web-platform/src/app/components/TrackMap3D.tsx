import { Canvas } from '@react-three/fiber'
import { Html, Line, OrbitControls } from '@react-three/drei'
import { Suspense, useMemo } from 'react'
import type { CarPositionDto, TrackLayoutResponseDto } from '@/api/types'
import {
  SECTOR_COLORS,
  computeBounds,
  computeThreeTransform,
  trackPointToWorld3D,
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

  const trackPointsForLine = useMemo(
    () =>
      layout.points
        .filter(p => p.x != null && (p.z != null || p.y != null))
        .map(p => {
          const w = trackPointToWorld3D(p)
          return worldToThree(w.x, w.y, w.z, transform)
        }),
    [layout.points, transform],
  )

  const elevRange = (bounds.maxElev ?? 0) - (bounds.minElev ?? 0)

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <Canvas camera={{ position: [0, 80, 120], fov: 50 }}>
        <Suspense fallback={null}>
          <ambientLight intensity={0.5} />
          <directionalLight position={[50, 100, 50]} intensity={0.8} />

          {trackPointsForLine.length > 1 && (
            <Line
              points={trackPointsForLine}
              color="#6B7280"
              lineWidth={4}
            />
          )}

        {sectors
          .filter(b => b.sector !== 1)
          .map(b => {
            const worldY = b.y ?? 0
            const worldZ = b.z ?? b.y!
            const pos = worldToThree(b.x, worldY, worldZ, transform)
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

        {/* S/F at start of sector 1: points[0] is the first point of the S1 segment */}
        {layout.points.length > 0 && (() => {
          const w = trackPointToWorld3D(layout.points[0])
          const pos = worldToThree(w.x, w.y, w.z, transform)
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
            car.worldPosY ?? bounds.minElev ?? 0,
            car.worldPosZ,
            transform,
          )
          const carColor = (car as { color?: string }).color ?? '#9CA3AF'
          return (
            <mesh key={car.carIndex} position={pos}>
              <sphereGeometry args={[1.5, 16, 16]} />
              <meshStandardMaterial
                color={carColor}
                emissive={carColor}
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
        </Suspense>
      </Canvas>

      {Number.isFinite(elevRange) && elevRange > 0.5 && (
        <p className="text-xs text-text-secondary mt-1 text-center">
          Elevation change: {elevRange.toFixed(1)} m (exaggerated in 3D for visibility)
        </p>
      )}
    </div>
  )
}

