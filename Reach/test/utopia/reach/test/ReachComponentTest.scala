package utopia.reach.test

import utopia.firmament.context.{ColorContext, TextContext}
import utopia.firmament.model.HotKey
import utopia.firmament.model.enumeration.StackLayout.Center
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.event.{KeyStateEvent, KeyTypedEvent}
import utopia.genesis.handling.{KeyStateListener, KeyTypedListener}
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorRole.{Primary, Secondary}
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.button.text.TextButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.text.EditableTextLabel
import utopia.reach.component.label.text.{ContextualMutableTextLabelFactory, MutableTextLabel, TextLabel}
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.stack.{ContextualStackFactory, Stack}
import utopia.reach.container.wrapper.Framing
import utopia.reach.focus.FocusListener
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.reflection.container.swing.window.{Frame, Window}
import utopia.firmament.localization.LocalString._
import utopia.firmament.model.stack.StackLength
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup

import java.awt.event.KeyEvent

/**
  * A simple test for the reach component implementation
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
object ReachComponentTest extends App
{
	import TestContext._
	import TestCursors._
	
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	def focusReporter(componentName: String) = FocusListener { event => println(s"$componentName: $event") }
	
	val windowPointer = Pointer[Option[Window[_]]](None)
	val result = ReachCanvas(cursors) { canvasHierarchy =>
		val (stack, _, label) = Stack(canvasHierarchy).withContext(baseContext.withStackMargins(StackLength.fixedZero))
			.build(Mixed).column() { factories =>
			
			val (framing, label) = factories.withoutContext(Framing).buildFilledWithMappedContext[ColorContext,
				TextContext, ContextualMutableTextLabelFactory](baseContext, colorScheme.secondary.light,
				MutableTextLabel) { _.forTextComponents.withTextAlignment(Alignment.Center) }
				.apply(margins.medium.any) { labelFactory =>
					labelFactory.withBackground("Hello!", Primary)
				}.toTuple
			
			// TODO: The second label "twitches" on content updates
			val label2 = factories.withContext(baseContext.against(colorScheme.primary)
				.forTextComponents.withTextAlignment(Alignment.Center))(TextLabel)
				.withCustomBackground("Hello 2\nThis label contains 2 lines", colorScheme.primary)
			
			val editLabelFraming = factories.withoutContext(Framing)
				.buildFilledWithMappedContext[ColorContext, TextContext, ContextualStackFactory](baseContext,
					colorScheme.primary.light, Stack) { _.forTextComponents.withTextAlignment(Alignment.Center) }
				.apply(margins.medium.any) {
					_.build(Mixed).column(Center) { factories =>
						val editableLabel = factories(EditableTextLabel)(new PointerWithEvents("Type Here"))
						editableLabel.addFocusListener(focusReporter("Label"))
						val buttonStack = factories(Stack).build(Mixed).row(areRelated = true) { factories =>
							val clearButton = factories.mapContext { _/Secondary }(TextButton)
								.apply("Clear (F1)", Set(HotKey.keyWithIndex(KeyEvent.VK_F1)),
									focusListeners = Vector(focusReporter("Clear Button"))) {
									editableLabel.text = ""
								}
							val closeButton = factories.mapContext { _/Primary }(TextButton)
								.apply("Close (esc)", Set(HotKey.keyWithIndex(KeyEvent.VK_ESCAPE)),
									focusListeners = Vector(focusReporter("Close Button"))) {
									windowPointer.value.foreach { _.close() }
								}
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
	
	println(s"Canvas stack size: ${ canvas.stackSize }")
	println(s"Label bounds: ${ label.bounds }")
	
	GlobalKeyboardEventHandler += KeyTypedListener { event: KeyTypedEvent => label.text += event.typedChar.toString }
	frame.addKeyStateListener(KeyStateListener(KeyStateEvent.keyFilter(KeyEvent.VK_BACK_SPACE)) { _ =>
		label.text = label.text.string.drop(1).noLanguageLocalizationSkipped
	})
}
