package utopia.reflection.test.swing

import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.async.process.Loop
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.color.{Color, Rgb}
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.container.swing.layout.multi.AnimatedCollectionView
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.firmament.model.stack.StackSize
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup

import scala.util.Random

/**
  * Tests collection view
  * @author Mikko Hilpinen
  * @since 16.1.2020, v1
  */
object CollectionViewTest extends App
{
	ParadigmDataType.setup()
	
	import TestContext._
	
	val collection = baseContext.use { implicit c => AnimatedCollectionView.contextual[AwtStackable](X, 480) }
	// new AnimatedCollectionView[AwtStackable](actorHandler, X, 480, 16.downscaling)
	// new CollectionView[AwtStackable](X, 480, 16.downscaling, forceEqualRowLength = true)
	val content = collection.alignedToSide(Direction2D.Left).framed(16.any x 16.any, Color.white) //.withAnimatedSize(actorHandler)
	
	new SingleFrameSetup(actorHandler, Frame.windowed(content, "Collection View Test", Program)).start()
	
	val random = new Random()
	Loop.regularly(1.seconds) {
		val label = new EmptyLabel
		label.background = Rgb(random.nextDouble(), random.nextDouble(), random.nextDouble())
		println("\nAdding a new label")
		collection += label.withStackSize(StackSize.any(Size(16 + random.nextInt(97), 64)))
	}
}
