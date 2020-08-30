package utopia.reflection.test

import utopia.flow.util.TimeExtensions._
import utopia.reflection.shape.LengthExtensions._
import utopia.flow.async.Loop
import utopia.genesis.color.{Color, RGB}
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.shape.Axis.X
import utopia.genesis.shape.shape2D.{Direction2D, Size}
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.layout.multi.AnimatedCollectionView
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.StackSize
import utopia.reflection.util.SingleFrameSetup

import scala.util.Random

/**
 * Tests collection view
 * @author Mikko Hilpinen
 * @since 16.1.2020, v1
 */
object CollectionViewTest extends App
{
	GenesisDataType.setup()
	
	import TestContext._
	
	val collection = baseContext.use { implicit c => AnimatedCollectionView.contextual[AwtStackable](X, 480) }
		// new AnimatedCollectionView[AwtStackable](actorHandler, X, 480, 16.downscaling)
	// new CollectionView[AwtStackable](X, 480, 16.downscaling, forceEqualRowLength = true)
	val content = collection.alignedToSide(Direction2D.Left).framed(16.any x 16.any, Color.white)//.withAnimatedSize(actorHandler)
	
	new SingleFrameSetup(actorHandler, Frame.windowed(content, "Collection View Test", Program)).start()
	
	val random = new Random()
	val createLabelLoop = Loop(1.seconds) {
		val label = new EmptyLabel
		label.background = RGB(random.nextDouble(), random.nextDouble(), random.nextDouble())
		collection += label.withStackSize(StackSize.any(Size(16 + random.nextInt(97), 64)))
	}
	createLabelLoop.registerToStopOnceJVMCloses()
	createLabelLoop.startAsync()
}
