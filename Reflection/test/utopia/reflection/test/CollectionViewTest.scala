package utopia.reflection.test

import utopia.flow.util.TimeExtensions._
import utopia.reflection.shape.LengthExtensions._
import utopia.flow.async.{Loop, ThreadPool}
import utopia.genesis.color.{Color, RGB}
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.shape2D.{Direction2D, Size}
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.container.swing.CollectionView
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.{StackInsets, StackSize}
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.util.{ComponentContext, ComponentContextBuilder, SingleFrameSetup}

import scala.concurrent.ExecutionContext
import scala.util.Random

/**
 * Tests collection view
 * @author Mikko Hilpinen
 * @since 16.1.2020, v1
 */
object CollectionViewTest extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates component context
	val actorHandler = ActorHandler()
	val baseCB = ComponentContextBuilder(actorHandler, Font("Arial", 12, Plain, 2), Color.green, Color.yellow, 320,
		insets = StackInsets.symmetric(8.any), stackMargin = 8.downscaling, relatedItemsStackMargin = Some(4.downscaling))
	
	implicit val baseContext: ComponentContext = baseCB.result
	
	val collection = new CollectionView[AwtStackable](Direction2D.Left, 320, 16.downscaling)
	val content = collection.alignedToSide(Direction2D.Left, useLowPriorityLength = true).framed(16.any x 16.any, Color.white)
	
	implicit val exc: ExecutionContext = new ThreadPool("Reflection").executionContext
	new SingleFrameSetup(actorHandler, Frame.windowed(content, "Collection View Test", Program)).start()
	
	val random = new Random()
	val createLabelLoop = Loop(1.seconds) {
		val label = new EmptyLabel
		label.background = RGB(random.nextDouble(), random.nextDouble(), random.nextDouble())
		collection += label.withStackSize(StackSize.any(Size(64, 64)))
	}
	createLabelLoop.registerToStopOnceJVMCloses()
	createLabelLoop.startAsync()
}
