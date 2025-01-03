package utopia.terra.test

import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.util.TryExtensions._
import utopia.paradigm.measurement.DistanceUnit.Meter
import utopia.paradigm.measurement.{Distance, DistanceUnit}
import utopia.terra.controller.coordinate.GlobeMath
import utopia.terra.controller.coordinate.world.SphericalEarth
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.sphere.SpherePoint

import scala.io.StdIn

/**
  * A simple application that calculates how much of the viewed object
  * should be hidden behind the curve on a spherical earth
  * @author Mikko Hilpinen
  * @since 03.01.2025, v1.2.1
  */
object HiddenHeightCalculator extends App
{
	// ATTRIBUTES   ------------------------
	
	private implicit val logger: Logger = SysErrLogger
	
	
	// APP CODE ----------------------------
	
	requestCoordinates("Please specify the observer's latitude-longitude coordinates, separated with ,")
		.foreach { observerCoordinates =>
			requestDistance(
				"Please specify how high the observer's eyes are from the mean sea level? E.g. 15.43m or 8.25ft")
				.foreach { observerAltitude =>
					requestCoordinates("Please specify the target's latitude-longitude coordinates, separated with ,")
						.foreach { targetCoordinates =>
							requestDistance(
								"Please specify how high the target's **lowest point** is from the mean sea level",
								observerAltitude.unit)
								.foreach { targetAltitude =>
									requestDistance("Finally, how high is the viewed object?", targetAltitude.unit)
										.foreach { targetHeight =>
											val (hidden, visible) = GlobeMath.calculateHiddenHeight(
												SpherePoint(observerCoordinates, observerAltitude),
												SpherePoint(targetCoordinates, targetAltitude), targetHeight)
											
											println()
											if (visible.isNegativeOrZero)
												println(s"The viewed object should be completely hidden, it's highest point ${
													hidden - targetHeight } under the horizon.")
											else
												println(s"$hidden of the viewed object should be hidden, $visible should remain visible")
												
											println(s"\nNote: All calculations assume a perfectly spherical Earth with a radius of ${
												SphericalEarth.globeRadius.distance }")
										}
								}
						}
				}
		}
	
	
	// OTHER    ----------------------------
	
	private def requestCoordinates(prompt: String) = {
		val coordinates = StdIn.readNonEmptyLine(prompt)
			.flatMap { _.tryPairWith { _.tryDouble }.log.map(LatLong.degrees) }
		if (coordinates.isEmpty)
			println("Invalid coordinates")
		coordinates
	}
	
	private def requestDistance(prompt: String, defaultUnit: DistanceUnit = Meter) = {
		val distance = StdIn.readNonEmptyLine(prompt).flatMap { Distance.parse(_, defaultUnit).log }
		if (distance.isEmpty)
			println("Invalid input")
		distance
	}
}
