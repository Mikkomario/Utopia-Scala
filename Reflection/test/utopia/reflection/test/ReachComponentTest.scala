package utopia.reflection.test

import java.awt.event.KeyEvent

import utopia.flow.datastructure.mutable.{Pointer, PointerWithEvents}
import utopia.genesis.color.Color
import utopia.genesis.event.{KeyStateEvent, KeyTypedEvent}
import utopia.genesis.handling.KeyStateListener
import utopia.reflection.color.ColorRole.Primary
import utopia.reflection.component.context.{BaseContext, ColorContext, TextContext}
import utopia.reflection.component.reach.button.TextButton
import utopia.reflection.component.reach.factory.Mixed
import utopia.reflection.component.reach.input.EditableTextLabel
import utopia.reflection.component.reach.label.{ContextualMutableTextLabelFactory, MutableTextLabel, TextLabel}
import utopia.reflection.container.reach.{ContextualStackFactory, Framing, Stack}
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.container.swing.window.{Frame, Window}
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.event.FocusListener
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
	
	def focusReporter(componentName: String) = FocusListener { event => println(s"$componentName: $event") }
	
	val windowPointer = new Pointer[Option[Window[_]]](None)
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
				.buildWithMappedContext[ColorContext, TextContext, ContextualStackFactory](Stack, baseContext) {
					_.forTextComponents.withTextAlignment(Alignment.Center) }
				.withBackground(colorScheme.primary.light, margins.medium.any) {
					_.build(Mixed).column(Center) { factories =>
						val editableLabel = factories(EditableTextLabel)(new PointerWithEvents("Type Here"))
						editableLabel.addFocusListener(focusReporter("Label"))
						val buttonStack = factories(Stack).build(Mixed).row(areRelated = true) { factories =>
							val clearButton = factories.mapContext { _.forSecondaryColorButtons }(TextButton)
								.apply("Clear (F1)", Set(KeyEvent.VK_F1),
									additionalFocusListeners = Vector(focusReporter("Clear Button"))) {
									editableLabel.text = "" }
							val closeButton = factories.mapContext { _.forPrimaryColorButtons }(TextButton)
								.apply("Close (esc)", Set(KeyEvent.VK_ESCAPE),
									additionalFocusListeners = Vector(focusReporter("Close Button"))) {
									windowPointer.value.foreach { _.close() } }
							Vector(clearButton, closeButton)
						}
						Vector(editableLabel, buttonStack.parent)
					}
				}.parent
			
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
	windowPointer.value = Some(frame)
	new SingleFrameSetup(actorHandler, frame).start()
	
	val label = result.result
	
	println(s"Canvas stack size: ${canvas.stackSize}")
	println(s"Label bounds: ${label.bounds}")
	
	frame.addKeyTypedListener { event: KeyTypedEvent => label.text += event.typedChar.toString }
	frame.addKeyStateListener(KeyStateListener(KeyStateEvent.keyFilter(KeyEvent.VK_BACK_SPACE)) { _ =>
		label.text = label.text.string.drop(1).noLanguageLocalizationSkipped })
}
