package utopia.terra.controller.coordinate.world

import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.vector.DoubleVector

/**
 * Common trait for world representation models / coordinate systems which represent the world / earth as
  * a level 2D surface, with altitude as the optional third dimension.
 * @author Mikko Hilpinen
 * @since 26.8.2024, v1.2
 * @tparam Surface Surface level (2D) point type
 * @tparam Aerial Aerial (3D) point type
 */
trait FlatWorldView[+Surface, +Aerial] extends WorldView[Vector2D, DoubleVector, Vector3D, Surface, Aerial]
