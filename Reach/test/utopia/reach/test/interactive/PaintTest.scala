package utopia.reach.test.interactive

import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.firmament.model.stack.StackSize
import utopia.flow.view.mutable.eventful.{CopyOnDemand, ResettableFlag}
import utopia.flow.time.TimeExtensions._
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.Loop
import utopia.paradigm.angular.Angle
import utopia.paradigm.color.{Color, Hsl}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.label.empty.ViewEmptyLabel
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

import scala.util.Random

/**
 * Tests basic painting functions
 * @author Mikko Hilpinen
 * @since 17.04.2026, v1.7.2
 */
object PaintTest extends App
{
	// ATTRIBUTES   ----------------------
	
	private val sizeChangeFlag = ResettableFlag()
	private val sizeP = sizeChangeFlag.map { flick =>
		val width = if (flick) 320 else 240
		StackSize.fixed(Size(width, 240))
	}
	
	private val colorP = CopyOnDemand { Hsl(Angle.circles(Random.nextDouble())): Color }
	
	private val window = ReachWindow.contentContextual.borderless.using(ViewEmptyLabel) { (_, labelF) =>
		labelF.withCustomDrawer(BackgroundViewDrawer(colorP))(sizeP)
	}
	
	
	// APP CODE --------------------------
	
	Loop.regularly(0.5.seconds, waitFirst = true) {
		colorP.update()
		window.content.repaintArea(Bounds(Point.twice(32), Size.square(64)))
	}
	Loop.regularly(2.1.seconds, waitFirst = true) { sizeChangeFlag.switch() }
	
	start()
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	
	window.closeFuture.waitFor()
	println("Done")
}
