package utopia.reach.test.interactive

import utopia.firmament.model.stack.StackLength
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.Pointer
import utopia.genesis.handling.event.keyboard.{KeyStateListener, KeyboardEvents}
import utopia.reach.component.factory.Mixed
import utopia.reach.component.visualization.{LoadingBar, ProgressBar}
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
  * Tests loading & progress bars
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
object BarTest extends App
{
	private val progressP = Pointer.eventful(0.5)
	
	private val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		framingF.build(Stack) { stackF =>
			stackF.build(Mixed) { factories =>
				val width = StackLength.any(320)
				val loading = factories(LoadingBar)(width)
				
				val progress1 = factories(ProgressBar).apply(progressP, width)
				val progress2 = factories(ProgressBar).slower.large.rounded(progressP, width)
				
				Vector(loading, progress1, progress2)
			}
		}
	}
	
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	private var lastBoundsTime = Now.toInstant
	window.boundsPointer.addListener { e =>
		val t = Now.toInstant
		val prev = lastBoundsTime
		lastBoundsTime = t
		println(s"${ e.newValue } (${ window.component.getBounds }) - ${ (t - prev).description }")
	}
	
	KeyboardEvents += KeyStateListener.pressed.anyDigit { _.digit.foreach { d => progressP.value = d * 0.1 } }
	
	start()
	window.display(centerOnParent = true)
}
