package utopia.reflection.test.swing

import utopia.firmament.context.ColorContext
import utopia.paradigm.animation.Animation
import utopia.reflection.component.swing.input.Slider
import utopia.reflection.component.swing.label.ItemLabel
import utopia.firmament.model.enumeration.StackLayout.Center
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.localization.DisplayFunction
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.firmament.model.stack.LengthExtensions._
import utopia.paradigm.color.ColorRole.Gray

/**
  * Tests different slider implementations
  * @author Mikko Hilpinen
  * @since 17.9.2020, v1.3
  */
object SliderTest extends App
{
	import TestContext._
	
	implicit val context: ColorContext = baseContext.against(colorScheme(Gray).light)
	val sliderWidth = 228.any.withLowPriority
	val defaultRange = Animation { p => p }
	val defaultOptions = Iterator.iterate(0.0) { _ + 0.2 }.takeWhile { _ <= 1.0 }.toVector
	val defaultColor = context.color.secondary
	val secondaryColor = context.color.primary
	
	val s1 = Slider.contextualSingleColor(defaultRange, sliderWidth, defaultColor)
	val s2 = Slider.contextualDualColor(defaultRange, sliderWidth, secondaryColor, defaultColor)
	val s3 = Slider.contextualSingleColorKnot(defaultRange, sliderWidth, defaultColor)
	val s4 = Slider.contextualSingleColorSelection(defaultOptions, sliderWidth, defaultColor)
	val s5 = Slider.contextualDualColorSelection(defaultOptions, sliderWidth, secondaryColor, defaultColor)
	val s6 = Slider.contextualSingleColorKnotSelection(defaultOptions, sliderWidth, defaultColor)
	
	def makeSliderRow(slider: Slider[Double]) =
		Stack.buildRowWithContext(layout = Center, isRelated = true) { s =>
			s += slider
			(context/Gray).forTextComponents.use { implicit c =>
				val label = ItemLabel.contextual(slider.value,
					DisplayFunction.noLocalization[Double] { p => (p * 100).toInt.toString })
				label.background = c.background
				// TODO: There should be an itemlabel variation that simply displays a pointer value without
				//  requiring mutability
				slider.valuePointer.addContinuousListener { e => label.content = e.newValue }
				s += label
			}
		}
	
	val content = Stack.columnWithItems(Vector(s1, s2, s3, s4, s5, s6).map(makeSliderRow), context.stackMargin)
		.framed(margins.medium.any, context.background)
	new SingleFrameSetup(actorHandler, Frame.windowed(content, "Slider Test")).start()
}
