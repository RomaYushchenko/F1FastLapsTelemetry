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
  elevationToColor,
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
    pts
      .filter(p => p.x != null && (p.z != null || p.y != null))
      .map(p => {
        const worldY = p.y ?? 0
        const worldZ = p.z ?? p.y!
        return worldToThree(p.x, worldY, worldZ, transform)
      })

  const coloredTrackPoints = useMemo(
    () =>
      layout.points
        .filter(p => p.x != null && (p.z != null || p.y != null))
        .map(p => {
          const worldY = p.y ?? 0
          const worldZ = p.z ?? p.y!
          return {
            position: worldToThree(p.x, worldY, worldZ, transform),
            color: elevationToColor(
              worldY,
              bounds.minElev ?? worldY,
              bounds.maxElev ?? worldY,
            ),
          }
        }),
    [layout.points, transform, bounds.minElev, bounds.maxElev],
  )

  const elevRange = bounds.maxElev - bounds.minElev

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <Canvas camera={{ position: [0, 80, 120], fov: 50 }}>
        <ambientLight intensity={0.5} />
        <directionalLight position={[50, 100, 50]} intensity={0.8} />

        {coloredTrackPoints.length > 1 && (
          <Line
            points={coloredTrackPoints.map(p => p.position)}
            colors={coloredTrackPoints.map(p => p.color)}
            vertexColors
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

        {sectors.find(b => b.sector === 1) && (() => {
          const s1 = sectors.find(b => b.sector === 1)!
          const worldY = s1.y ?? 0
          const worldZ = s1.z ?? s1.y!
          const pos = worldToThree(s1.x, worldY, worldZ, transform)
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

      {Number.isFinite(elevRange) && elevRange > 0.5 && (
        <p className="text-xs text-text-secondary mt-1 text-center">
          Elevation change: {elevRange.toFixed(1)} m
        </p>
      )}
    </div>
  )
}

