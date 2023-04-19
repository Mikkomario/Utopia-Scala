package utopia.reach.test

import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.image.SingleColorIconCache
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.{RowGroup, RowGroups}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.flow.view.template.eventful.FlagLike.wrap
import utopia.genesis.util.ScreenExtensions._
import utopia.paradigm.color.ColorRole
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.measurement.DistanceExtensions._
import utopia.paradigm.shape.shape2d.Size
import utopia.reach.component.factory.ContextualMixed
import utopia.reach.component.input.check.CheckBox
import utopia.reach.component.input.selection.RadioButtonGroup
import utopia.reach.component.input.text.{DurationField, TextField}
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.OpenComponent
import utopia.reach.container.multi.{Stack, ViewStack}
import utopia.reach.container.wrapper.Framing
import utopia.reach.context.{ReachContentWindowContext, ReachWindowContext}
import utopia.reach.focus.FocusRequestable
import utopia.reach.window.InputField._
import utopia.reach.window.{InputRowBlueprint, InputWindowFactory}

/**
  * Tests input window creation
  * @author Mikko Hilpinen
  * @since 6.3.2021, v0.1
  */
object InputWindowTest extends App
{
	import ReachTestContext._
	
	val icons = new SingleColorIconCache("Reach/test-images", Some(Size.square(32)))
	val selectedBoxIcon = icons("check-box-selected.png")
	val unselectedBoxIcon = icons("check-box-empty.png")
	println(selectedBoxIcon.size)
	
	object TestWindows extends InputWindowFactory[Model, Unit]
	{
		// ATTRIBUTES	-------------------------
		
		private val defaultFieldWidth = 5.cm.toScreenPixels.any
		
		override protected lazy val closeIcon = icons("close.png")
		
		override protected lazy val standardContext = baseContext.against(colors.primary)
		override protected lazy val fieldCreationContext = baseContext.against(colors.primary.light)
		
		
		// IMPLEMENTED	-------------------------
		
		override protected def windowContext: ReachWindowContext = ReachTestContext.windowContext
		override protected def warningPopupContext: ReachContentWindowContext =
			windowContext.borderless.nonResizable.withContentContext(baseContext.against(colors.failure).forTextComponents)
		override protected def log: Logger = ReachTestContext.log
		
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
			base.forTextComponents.mapFont { _ * 0.8 } -> base.forTextComponents.singleLine
		
		override protected def buildLayout(factories: ContextualMixed[ColorContext],
		                                   content: Vector[OpenComponent[ReachComponentLike, Changing[Boolean]]],
		                                   context: Unit) =
		{
			val framingMargin = margins.medium.downscaling x margins.medium.any
			
			// Frames content
			if (content.size == 1)
				factories(Framing).withoutContext(content.head, framingMargin,
					Vector(BackgroundDrawer(fieldCreationContext.background)))
			// If there are many, wraps them in a stack also
			else if (content.forall { _.result.isAlwaysTrue })
				factories(Stack).build(Framing).column() { framingF =>
					content.map { c => framingF.withoutContext(c, framingMargin,
						Vector(BackgroundDrawer(fieldCreationContext.background))) }
				}
			else
				factories(ViewStack).build(Framing).withFixedStyle() { factories =>
					content.map { c => factories.next().withoutContext(c, framingMargin,
						Vector(BackgroundDrawer(fieldCreationContext.background))).parentAndResult }
				}
		}
		
		override protected def specifyButtons(context: Unit,
											  input: => Either[(String, ReachComponentLike with FocusRequestable), Model],
											  warn: (String, LocalizedString) => Unit) =
		{
			val okButton = model.WindowButtonBlueprint[Model]("OK", role = Secondary, isDefault = true) { promise =>
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
					.mapTextInsets { _.mapLeft { _ * 0.5 }.mapRight { _ * 1.5 }.expandingToRight }
					.withBackground(buttonColor)
			else
				standardContext.forTextComponents.withTextAlignment(Alignment.Center)
					.mapTextInsets { _.mapHorizontal { _ * 1.5 } }
					.withBackground(buttonColor)
		}
		
		override protected def defaultResult = Model.empty
		
		override protected def title = "Test"
	}
	
	// Displays a dialog
	start()
	val result = TestWindows.displayBlocking().get
	println(s"Dialog completed with result: $result")
	
	println(result("durationSeconds").getLong.seconds.description)
}
