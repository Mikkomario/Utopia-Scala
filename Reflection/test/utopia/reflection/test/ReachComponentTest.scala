package utopia.reflection.test

import java.awt.event.KeyEvent

import utopia.genesis.color.Color
import utopia.genesis.event.{KeyStateEvent, KeyTypedEvent}
import utopia.genesis.handling.KeyStateListener
import utopia.reflection.color.ColorRole.{Gray, Primary}
import utopia.reflection.color.ColorShade.Light
import utopia.reflection.component.context.{BaseContext, ColorContext, TextContext}
import utopia.reflection.component.reach.label.MutableTextLabel
import utopia.reflection.container.reach.Framing
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.localization.LocalString._

/**
  * A simple test for the reach component implementation
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
object ReachComponentTest extends App
{
	import TestContext._
	
	val result = ReachCanvas { canvasHierarchy =>
		val (framing, label) = Framing.withBackground(canvasHierarchy, colorScheme.secondary.light,
			StackInsets.symmetric(margins.medium.any))
		{
			(framingH, c) =>
				implicit val context: TextContext = c.forTextComponents(Alignment.Center)
				MutableTextLabel.contextualWithBackground(framingH, "Hello!", Primary)
		}(baseContext).toTuple
		
		framing -> label
	}
	val canvas = result.parent
	canvas.background = Color.magenta
	
	val frame = Frame.windowed(canvas, "Reach Test", Program)
	new SingleFrameSetup(actorHandler, frame).start()
	
	val label = result.result
	
	println(s"Canvas stack size: ${canvas.stackSize}")
	println(s"Label bounds: ${label.bounds}")
	
	frame.addKeyTypedListener { event: KeyTypedEvent => label.text += event.typedChar.toString }
	frame.addKeyStateListener(KeyStateListener(KeyStateEvent.keyFilter(KeyEvent.VK_BACK_SPACE)) { _ =>
		label.text = label.text.string.drop(1).noLanguageLocalizationSkipped })
}
