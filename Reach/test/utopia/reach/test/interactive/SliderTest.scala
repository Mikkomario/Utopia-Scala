package utopia.reach.test.interactive

import utopia.firmament.drawing.immutable.BorderDrawer
import utopia.firmament.model.Border
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.SettableOnce
import utopia.genesis.handling.event.mouse.MouseMoveListener
import utopia.paradigm.color.Color
import utopia.reach.component.input.selection.Slider
import utopia.reach.container.wrapper.{Framing, Rotated}
import utopia.reach.drawing.MousePositionDrawer
import utopia.reach.window.ReachWindow

/**
  * A simple test for the slider component
  * @author Mikko Hilpinen
  * @since 16.08.2024, v1.4
  */
object SliderTest extends App
{
	import utopia.reach.test.ReachTestContext._
	
	private val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		framingF.build(Rotated) { rotatedF =>
			rotatedF.counterClockwise.build(Slider) { sliderF =>
				val sliderPointer = SettableOnce[Slider[_]]()
				// val mouseDrawer = new MousePositionDrawer(sliderPointer)
				
				val slider = sliderF
					// .withCustomDrawer(BorderDrawer(Border(1.0, Color.red)))
					// .withCustomDrawer(mouseDrawer)
					.forDoubles(NumericSpan(1.0, 10.0))(360.any, 1.0)
				sliderPointer.set(slider)
				// slider.addMouseMoveListener(MouseMoveListener { e => println(e.position.relative) })
				
				slider
			}
		}
		/*
		framingF.build(Slider) { sliderF =>
			val slider = sliderF
				// .withAnimationDuration(1.seconds)
				// .withCustomDrawer(BorderDrawer(Border(1.0, Color.red)))
				.forDoubles(NumericSpan(1.0, 10.0))(360.any, 1.0)
			// slider.valuePointer.addListener { e => println(e.newValue) }
			slider
		}*/
	}
	
	window.setToExitOnClose()
	window.display(centerOnParent = true)
	start()
}
