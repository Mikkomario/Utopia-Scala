package utopia.reflection.test

import java.awt.event.KeyEvent

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.genesis.event.{KeyStateEvent, KeyTypedEvent}
import utopia.genesis.handling.KeyStateListener
import utopia.reflection.color.ColorRole.Primary
import utopia.reflection.component.context.{ColorContext, TextContext}
import utopia.reflection.component.reach.factory.Mixed
import utopia.reflection.component.reach.input.EditableTextLabel
import utopia.reflection.component.reach.label.{ContextualMutableTextLabelFactory, MutableTextLabel, TextLabel}
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
		val (stack, _, label) = Stack(canvasHierarchy).withContext(baseContext.withStackMargin(StackLength.fixedZero))
			.build(Mixed).column() { factories =>
			
			val (framing, label) = factories.withoutContext(Framing).buildWithMappedContext[ColorContext,
				TextContext, ContextualMutableTextLabelFactory](MutableTextLabel, baseContext) {
				_.forTextComponents.withTextAlignment(Alignment.Center) }
				.withBackground(colorScheme.secondary.light, margins.medium.any) { labelFactory =>
					labelFactory.withBackground("Hello!", Primary)
				}.toTuple
			
			// TODO: The second label "twitches" on content updates
			val label2 = factories.withContext(baseContext.inContextWithBackground(colorScheme.primary)
				.forTextComponents.withTextAlignment(Alignment.Center))(TextLabel)
				.withCustomBackground("Hello 2\nThis label contains 2 lines", colorScheme.primary)
			
			val editLabelFraming = factories.withoutContext(Framing)
				.buildWithMappedContext(EditableTextLabel, baseContext) {
					_.forTextComponents.withTextAlignment(Alignment.Center) }
				.withBackground(colorScheme.primary.light, margins.medium.any) {
					_(new PointerWithEvents("Type Here")) }.parent
			
			Vector(framing, label2, editLabelFraming) -> label
		}.toTriple
		
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
