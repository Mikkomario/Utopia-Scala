package utopia.terra.test

import utopia.flow.operator.Use
import utopia.paradigm.measurement.DistanceExtensions._
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.terra.controller.coordinate.world.GridArea
import utopia.terra.model.angular.LatLong
import utopia.terra.model.enumeration.CompassDirection.{East, North, South, West}
import utopia.terra.model.world.grid.GridSurfacePoint

/**
  * Tests the grid-based world-view
  * @author Mikko Hilpinen
  * @since 3.11.2023, v1.0.1
  */
object GridAreaTest extends App
{
	// import utopia.flow.test.TestContext._
	
	private val unit = GridArea.oneDegreeLatitudeArcVectorLength
	
	Use(new GridArea(LatLong.origin)) { implicit grid =>
		val northVector = grid.latLongToVector(LatLong(North.degrees(1.0), Angle.zero))
		assert(northVector ~== Vector2D(-unit))
		val southVector = grid.latLongToVector(LatLong(South.degrees(1.0), Angle.zero))
		assert(southVector ~== Vector2D(unit))
		val eastVector = grid.latLongToVector(LatLong(Rotation.zero, East.degrees(1.0).toAngle))
		assert(eastVector ~== Vector2D(0.0, -unit))
		val westVector = grid.latLongToVector(LatLong(Rotation.zero, West.degrees(1.0).toAngle))
		assert(westVector ~== Vector2D(0.0, unit))
		
		val northLatLong = grid.vectorToLatLong(Vector2D(-unit))
		assert(northLatLong ~== LatLong(North.degrees(1.0)))
		val southLatLong = grid.vectorToLatLong(Vector2D(unit))
		assert(southLatLong ~== LatLong(South.degrees(1.0)))
		val eastLatLong = grid.vectorToLatLong(Vector2D(0.0, -unit))
		assert(eastLatLong ~== LatLong(longitude = East.degrees(1.0).toAngle))
		val westLatLong = grid.vectorToLatLong(Vector2D(0.0, unit))
		assert(westLatLong ~== LatLong(longitude = West.degrees(1.0).toAngle))
		
		val origin2 = LatLong(North.degrees(45))
		val grid2 = new GridArea(origin2)
		println(grid2.latLongToVector(origin2 + North.degrees(1.0)))
		println(grid2.latLongToVector(origin2 + West.degrees(1.0)))
		// println(grid2.latLongToVector(LatLong(North.degrees(90), East.degrees(45.0).toAngle)))
		
		val origin = GridSurfacePoint(Vector2D(0, 0))
		val oneDegreeLatitudeNorth = GridSurfacePoint(LatLong(North.degrees(1.0), Angle.zero))
		val oneDegreeLatitudeSouth = GridSurfacePoint(LatLong(South.degrees(1.0), Angle.zero))
		val oneDegreeLongEast = GridSurfacePoint(LatLong(Rotation.zero, East.degrees(1.0).toAngle))
		
		println(s"1 degree latitude N = ${ oneDegreeLatitudeNorth.vector }")
		println(s"1 degree latitude S = ${ oneDegreeLatitudeSouth.vector }")
		println(s"1 degree longitude E = ${ oneDegreeLongEast.vector }")
		
		assert(origin.latLong == LatLong.origin)
		assert(oneDegreeLatitudeNorth.vector ~== Vector2D(-unit))
		assert(oneDegreeLatitudeSouth.vector ~== Vector2D(unit))
	}
	
	val helsinkiLatLong = LatLong.fromDegrees(60.192059, 24.945831)
	Use(new GridArea(helsinkiLatLong)) { implicit grid =>
		val helsinki = GridSurfacePoint(helsinkiLatLong)
		val lahti = GridSurfacePoint(LatLong.fromDegrees(60.9827, 25.6615))
		
		println(s"Distance from Helsinki to Lahti is ${ lahti.linearDistanceFrom(helsinki) }")
		
		val aerialLahti = lahti.withAltitude(96.km)
		
		println(s"Aerial Lahti point: $aerialLahti")
		// println(s"Vector distance between Lahti and Helsinki = ${lahti - helsinki}")
	}
	
	val cloncurryLatLong = LatLong.fromDegrees(-20.7110, 140.5050)
	Use(new GridArea(cloncurryLatLong)) { implicit grid =>
		val cloncurry = GridSurfacePoint(cloncurryLatLong)
		val richmond = GridSurfacePoint(LatLong.fromDegrees(-20.7375, 143.1290))
		
		println(s"Distance from Cloncurry to Richmond is ${ cloncurry.linearDistanceFrom(richmond) }")
	}
}
