package utopia.genesis.test

import utopia.flow.collection.immutable.Matrix
import utopia.flow.parse.file.FileExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * @author Mikko Hilpinen
  * @since 31.8.2023, v
  */
object RepeatingPixelsTest extends App
{
	ParadigmDataType.setup()
	
	val pixels = Image.readFrom("Genesis/test-images/mushrooms.png").get.pixels
	
	val runTime = Runtime.getRuntime
	val maxMemory = runTime.maxMemory()
	
	def printMemoryStatus() = {
		val used = runTime.totalMemory()
		println(s"${ used * 100 / maxMemory }% (${ used / 100000 } M) used")
	}
	
	printMemoryStatus()
	(1 until 100000).foreach { _ =>
		Color.fromInt(0)
		// pixels.view(Bounds(Point(10, 10), Size(20, 20)))
	}
	printMemoryStatus()
}
