package utopia.reflection.test.swing

import utopia.paradigm.animation.Animation
import utopia.reflection.color.ColorRole.Gray
import utopia.reflection.component.context.ColorContext
import utopia.reflection.component.swing.input.Slider
import utopia.reflection.component.swing.label.ItemLabel
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._

/**
  * Tests different slider implementations
  * @author Mikko Hilpinen
  * @since 17.9.2020, v1.3
  */
object SliderTest extends App
{
	
	import TestContext._
	
	implicit val context: ColorContext = baseContext.inContextWithBackground(colorScheme.gray.light)
	val sliderWidth = 228.any.withLowPriority
	val defaultRange = Animation { p => p }
	val defaultOptions = Iterator.iterate(0.0) { _ + 0.2 }.takeWhile { _ <= 1.0 }.toVector
	val defaultColor = colorScheme.secondary.forBackground(context.containerBackground)
	val secondaryColor = colorScheme.primary.forBackground(context.containerBackground)
	
	val s1 = Slider.contextualSingleColor(defaultRange, sliderWidth, defaultColor)
	val s2 = Slider.contextualDualColor(defaultRange, sliderWidth, secondaryColor, defaultColor)
	val s3 = Slider.contextualSingleColorKnot(defaultRange, sliderWidth, defaultColor)
	val s4 = Slider.contextualSingleColorSelection(defaultOptions, sliderWidth, defaultColor)
	val s5 = Slider.contextualDualColorSelection(defaultOptions, sliderWidth, secondaryColor, defaultColor)
	val s6 = Slider.contextualSingleColorKnotSelection(defaultOptions, sliderWidth, defaultColor)
	
	def makeSliderRow(slider: Slider[Double]) =
		Stack.buildRowWithContext(layout = Center, isRelated = true) { s =>
			s += slider
			context.forChildComponentWithRole(Gray).forTextComponents.use { implicit c =>
				val label = ItemLabel.contextual(slider.value,
					DisplayFunction.noLocalization[Double] { p => (p * 100).toInt.toString })
				label.background = c.containerBackground
				// TODO: There should be an itemlabel variation that simply displays a pointer value without
				//  requiring mutability
				slider.valuePointer.addListener { e => label.content = e.newValue }
				s += label
			}
		}
	
	val content = Stack.columnWithItems(Vector(s1, s2, s3, s4, s5, s6).map(makeSliderRow), context.defaultStackMargin)
		.framed(margins.medium.any, context.containerBackground)
	new SingleFrameSetup(actorHandler, Frame.windowed(content, "Slider Test")).start()
}
