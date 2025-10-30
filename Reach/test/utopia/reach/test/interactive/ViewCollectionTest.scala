package utopia.reach.test.interactive

import utopia.firmament.model.enumeration.SizeCategory.{Medium, Small}
import utopia.firmament.model.stack.StackSize
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.Identity
import utopia.flow.view.mutable.Pointer
import utopia.genesis.handling.event.keyboard.{KeyStateListener, KeyboardEvents}
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.multi.ViewCollection
import utopia.reach.container.wrapper.AlignFrame
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
 * @author Mikko Hilpinen
 * @since 30.10.2025, v1.7
 */
object ViewCollectionTest extends App
{
	private val endP = Pointer.eventful(5)
	private val startP = endP.incrementalMap(Identity) { (_, change) => change.oldValue }
	private val valuesP = startP.mergeWith(endP) { (s, e) => NumericSpan(s, e).iterator.toOptimizedSeq }
	
	private val window = ReachWindow.contentContextual.borderless.using(AlignFrame) { (_, frameF) =>
		val frame = frameF.center.build(ViewCollection) { collF =>
			collF.withOuterMargin(Medium).withInnerMargin(Small).withSplitThreshold(400)
				.withBackground(Color.white)
				.mapPointer(valuesP, ViewTextLabel) { (labelF, p, _) =>
					labelF.withTextInsets(Medium).withBackground(Color.red)(p)
				}
		}
		frame.addConstraint { s => s max StackSize.fixed(Size.square(500)) }
		frame
	}
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	
	KeyboardEvents += KeyStateListener.pressed.anyDigit { e => e.digit.foreach { endP.value = _ } }
	
	start()
	window.display(centerOnParent = true)
	
	window.closeFuture.waitFor()
	println("Closing")
}
