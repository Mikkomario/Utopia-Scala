package utopia.reach.test

import utopia.firmament.model.enumeration.SizeCategory.Large
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.CollectionExtensions._
import utopia.genesis.util.Screen
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.container.multi.Collection
import utopia.reach.window.ReachWindow

import scala.util.Random

/**
  * Tests static collection class
  * You should be able to see a window with colored boxes in it.
  * @author Mikko Hilpinen
  * @since 4.5.2023, v1.1
  */
object ReachCollectionTest extends App
{
	import ReachTestContext._
	
	val colorOptions = colors.definedRoles.toVector
	val window = ReachWindow.contentContextual.using(Collection) { (_, collF) =>
		collF.withOuterMargin(Large).withSplitThreshold(Screen.width * 0.5).build(EmptyLabel) { labelF =>
			Vector.fill(20) {
				labelF.withBackground(colorOptions.random).apply(
					StackSize.upscaling(Size.square(16),
						Size.square(16) + Size.square(Random.nextDouble() * 228)).lowPriority) }
		}
	}
	
	window.setToExitOnClose()
	window.display(centerOnParent = true)
	start()
}
