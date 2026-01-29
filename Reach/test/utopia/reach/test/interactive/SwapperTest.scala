package utopia.reach.test.interactive

import utopia.firmament.model.stack.StackSize
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.paradigm.color.Color
import utopia.paradigm.measurement.DistanceExtensions._
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.factory.Mixed
import utopia.reach.component.interactive.button.text.TextButton
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.{Framing, Swapper}
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
 * Tests the Swapper view
 * @author Mikko Hilpinen
 * @since 29.01.2026, v1.7.1
 */
object SwapperTest extends App
{
	private val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		framingF.build(Stack) { stackF =>
			stackF.centeredRow.build(Mixed) { factories =>
				val p = ResettableFlag()
				val swapper = factories(Swapper).notCaching.mixed.apply(p) { (factories, value) =>
					factories(EmptyLabel).withBackground(if (value) Color.red else Color.blue)
						.apply(StackSize.any(Size.square(2.cm.toPixels)))
				}
				val button = factories(TextButton).primary.apply("Swap") { p.switch() }
				
				Pair(swapper, button)
			}
		}
	}
	
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	start()
	window.display(centerOnParent = true)
	window.closeFuture.waitFor()
}
