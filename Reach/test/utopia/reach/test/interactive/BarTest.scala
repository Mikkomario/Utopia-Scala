package utopia.reach.test.interactive

import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.Pair
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.Pointer
import utopia.genesis.handling.event.keyboard.{KeyStateListener, KeyboardEvents}
import utopia.paradigm.color.Color
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
				val context = factories.context
				val width = StackLength.any(320)
				val loading = factories(LoadingBar)(width)
				
				val progress1 = factories(ProgressBar).apply(progressP, width)
				val progress2 = factories(ProgressBar).slower.large.rounded(progressP, width)
				val progress3 = factories(ProgressBar).rounded.small
					.withColorFunction { (progress, bg) =>
						// The bar goes from green to red
						val baseBarColor = Color.green.average(Color.red, progress, 1 - progress)
						val actualBarColor =  {
							if (baseBarColor.contrastAgainst(bg) >= context.requiredContrast.largeTextMin)
								baseBarColor
							else {
								val variantsIter = {
									if (bg.relativeLuminance > baseBarColor.relativeLuminance)
										(1 to 2).iterator.map { i => baseBarColor.darkenedBy(i / 2) }
									else
										(1 to 2).iterator.map { i => baseBarColor.lightenedBy(i / 2) }
								}
								variantsIter
									.find { _.contrastAgainst(bg) > context.requiredContrast.largeTextMin }
									.getOrElse(baseBarColor)
							}
						}
						
						val bgColor = context.colors.gray.againstMany(Pair(bg, actualBarColor))
						Pair(actualBarColor, bgColor)
					}
					.apply(progressP, width)
				
				Vector(loading, progress1, progress2, progress3)
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
	
	KeyboardEvents += KeyStateListener.pressed.anyDigit { _.digit.foreach { d => progressP.value = d / 9.0 } }
	
	start()
	window.display(centerOnParent = true)
}
