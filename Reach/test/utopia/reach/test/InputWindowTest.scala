package utopia.reach.test

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.flow.view.template.eventful.FlagLike.wrap
import utopia.genesis.handling.ActorLoop
import utopia.genesis.util.ScreenExtensions._
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.measurement.DistanceExtensions._
import utopia.paradigm.shape.shape2d.Size
import utopia.reach.component.factory.ContextualMixed
import utopia.reach.component.input.check.CheckBox
import utopia.reach.component.input.selection.RadioButtonGroup
import utopia.reach.component.input.text.{DurationField, TextField}
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.OpenComponent
import utopia.reach.container.multi.stack.{Stack, ViewStack}
import utopia.reach.container.wrapper.Framing
import utopia.reach.focus.FocusRequestable
import utopia.reach.window.InputField._
import utopia.reach.window.{InputRowBlueprint, InputWindowFactory}
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.ColorContext
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.template.window.{RowGroup, RowGroups, WindowButtonBlueprint}
import utopia.reflection.image.SingleColorIconCache
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.LengthExtensions._

/**
  * Tests input window creation
  * @author Mikko Hilpinen
  * @since 6.3.2021, v0.1
  */
object InputWindowTest extends App
{
	ParadigmDataType.setup()
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	import TestCursors._
	import utopia.reflection.test.TestContext._
	
	val icons = new SingleColorIconCache("Reach/test-images", Some(Size.square(32)))
	val selectedBoxIcon = icons("check-box-selected.png")
	val unselectedBoxIcon = icons("check-box-empty.png")
	println(selectedBoxIcon.size)
	
	object TestWindows extends InputWindowFactory[Model, Unit]
	{
		// ATTRIBUTES	-------------------------
		
		private val defaultFieldWidth = 5.cm.toScreenPixels.any
		
		override protected lazy val closeIcon = icons("close.png")
		
		override protected lazy val standardContext = baseContext.inContextWithBackground(colorScheme.primary)
		
		override protected lazy val fieldCreationContext =
			baseContext.inContextWithBackground(colorScheme.primary.light)
		
		
		// IMPLEMENTED	-------------------------
		
		override protected def warningPopupContext =
			baseContext.inContextWithBackground(colorScheme.error).forTextComponents
		
		override protected def inputTemplate =
		{
			val nameErrorPointer = new PointerWithEvents(LocalizedString.empty)
			val firstNameField = InputRowBlueprint.using(TextField, "firstName", fieldAlignment = Alignment.Center) { fieldF =>
				val textPointer = new PointerWithEvents[String]("")
				val displayErrorPointer = nameErrorPointer.mergeWith(textPointer) { (error, text) =>
					if (text.isEmpty) error else LocalizedString.empty
				}
				val field = fieldF.forString(defaultFieldWidth, Fixed("First Name"),
					errorMessagePointer = displayErrorPointer, textPointer = textPointer)
				field.validateWith { s =>
					if (s.isEmpty) {
						nameErrorPointer.value = "Required Field"
						"Please insert a value first"
					}
					else
						LocalizedString.empty
				}
			}
			val lastNameField = InputRowBlueprint.using(TextField, "lastName", fieldAlignment = Alignment.Center) {
				_.forString(defaultFieldWidth, Fixed("Last Name"), hintPointer = Fixed("Optional")) }
			val sexField = InputRowBlueprint.using(RadioButtonGroup, "isMale", "Sex",
				Alignment.BottomLeft) { _(Vector[(Boolean, LocalizedString)](true -> "Male", false -> "Female")) }
			
			val durationField = InputRowBlueprint.using(DurationField, "durationSeconds",
				"Login Duration") { _.apply(maxValue = 24.hours).convertWith { _.toSeconds }
			}
			
			val acceptTermsField = InputRowBlueprint.using(CheckBox, "accept",
				"I accept the terms and conditions of use", fieldAlignment = Alignment.Left,
				isScalable = false) {
					_(selectedBoxIcon, unselectedBoxIcon).validateWith {
						if (_) LocalizedString.empty else "You must accept the terms and conditions to continue"
					}
			}
			
			Vector(RowGroups(RowGroup(firstNameField, lastNameField),
				RowGroup.singleRow(sexField), RowGroup.singleRow(durationField)),
				RowGroups.singleRow(acceptTermsField)) -> ()
		}
		
		override protected def makeFieldNameAndFieldContext(base: ColorContext) =
			base.forTextComponents.mapFont { _ * 0.8 } -> base.forTextComponents.noLineBreaksAllowed
		
		override protected def buildLayout(factories: ContextualMixed[ColorContext],
		                                   content: Vector[OpenComponent[ReachComponentLike, Changing[Boolean]]],
		                                   context: Unit) =
		{
			val framingMargin = margins.medium.downscaling x margins.medium.any
			
			// Frames content
			if (content.size == 1)
				factories(Framing).withoutContext(content.head, framingMargin,
					Vector(BackgroundDrawer(fieldCreationContext.containerBackground)))
			// If there are many, wraps them in a stack also
			else if (content.forall { _.result.isAlwaysTrue })
				factories(Stack).build(Framing).column() { framingF =>
					content.map { c => framingF.withoutContext(c, framingMargin,
						Vector(BackgroundDrawer(fieldCreationContext.containerBackground))) }
				}
			else
				factories(ViewStack).build(Framing).withFixedStyle() { factories =>
					content.map { c => factories.next().withoutContext(c, framingMargin,
						Vector(BackgroundDrawer(fieldCreationContext.containerBackground))).parentAndResult }
				}
		}
		
		override protected def specifyButtons(context: Unit,
											  input: => Either[(String, ReachComponentLike with FocusRequestable), Model],
											  warn: (String, LocalizedString) => Unit) =
		{
			val okButton = WindowButtonBlueprint[Model]("OK", role = Secondary, isDefault = true) { promise =>
				input.toOption.foreach(promise.trySuccess)
			}
			Vector(okButton) -> AlwaysTrue
		}
		
		override protected def defaultCloseButtonText = "Cancel"
		
		override protected def executionContext = exc
		
		override protected def buttonContext(buttonColor: ColorRole, hasIcon: Boolean) =
		{
			if (hasIcon)
				standardContext.forTextComponents.withTextAlignment(Alignment.Left)
					.mapInsets { _.mapLeft { _ * 0.5 }.mapRight { _ * 1.5 }.expandingToRight }
					.forButtons(buttonColor)
			else
				standardContext.forTextComponents.withTextAlignment(Alignment.Center)
					.mapInsets { _.mapHorizontal { _ * 1.5 } }
					.forButtons(buttonColor)
		}
		
		override protected def defaultResult = Model.empty
		
		override protected def title = "Test"
	}
	
	// Starts events
	val actionLoop = new ActorLoop(actorHandler)
	actionLoop.runAsync()
	StackHierarchyManager.startRevalidationLoop()
	
	// Displays a dialog
	val result = TestWindows.displayBlocking(cursors = cursors).get
	println(s"Dialog completed with result: $result")
	
	println(result("durationSeconds").getLong.seconds.description)
}
