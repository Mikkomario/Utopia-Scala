package utopia.reflection.test

import java.awt.event.KeyEvent

import utopia.genesis.color.Color
import utopia.genesis.event.{KeyStateEvent, KeyTypedEvent}
import utopia.genesis.handling.KeyStateListener
import utopia.reflection.color.ColorRole.Primary
import utopia.reflection.component.reach.factory.Mixed
import utopia.reflection.component.reach.label.{MutableTextLabel, StaticTextLabel}
import utopia.reflection.container.reach.{Framing, Stack}
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.stack.StackLength

/**
  * A simple test for the reach component implementation
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
object ReachComponentTest extends App
{
	import TestContext._
	
	val result = ReachCanvas { canvasHierarchy =>
		// TODO: Handle context passing better
		val (stack, _, label) = Stack(canvasHierarchy).builder(Mixed).column() { factories =>
			val (framing, label) = factories(Framing).builder(MutableTextLabel)
				.withBackground(colorScheme.secondary.light, margins.medium.any) { (labelFactory, context) =>
					context.forTextComponents(Alignment.Center).use { implicit context =>
						labelFactory.withBackground("Hello!", Primary)
					}
				}(baseContext).toTuple // TODO: Add method for stack margin override
			// TODO: The second label "twitches" on content updates
			val label2 = factories(StaticTextLabel).withContext(baseContext.inContextWithBackground(colorScheme.primary)
				.forTextComponents(Alignment.Center)).withCustomBackground("Hello 2", colorScheme.primary)
			Vector(framing, label2) -> label
		}(baseContext.copy(stackMarginOverride = Some(StackLength.fixedZero))).toTriple
		
		stack -> label
		
		/*
		Framing(canvasHierarchy).builder(MutableTextLabel).withBackground(
			colorScheme.secondary.light, margins.medium.any)
		{
			(labelFactory, c) =>
				implicit val context: TextContext = c.forTextComponents(Alignment.Center)
				labelFactory.withBackground("Hello!", Primary)
		}(baseContext).toTuple*/
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
