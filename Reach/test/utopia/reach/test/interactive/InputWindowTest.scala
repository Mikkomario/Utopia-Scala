package utopia.reach.test.interactive

import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.image.ImageCache
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.{RowGroup, RowGroups}
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing
import utopia.flow.view.template.eventful.Flag.wrap
import utopia.genesis.util.ScreenExtensions._
import utopia.paradigm.color.ColorRole
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.BottomLeft
import utopia.paradigm.measurement.DistanceExtensions._
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.factory.ContextualMixed
import utopia.reach.component.input.check.CheckBox
import utopia.reach.component.input.selection.RadioButtonGroup
import utopia.reach.component.input.text.{DurationField, TextField}
import utopia.reach.component.template.ReachComponent
import utopia.reach.component.wrapper.OpenComponent
import utopia.reach.container.multi.{Stack, ViewStack}
import utopia.reach.container.wrapper.Framing
import utopia.reach.focus.FocusRequestable
import utopia.reach.test.ReachTestContext
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.InputField._
import utopia.reach.window.{InputRowBlueprint, InputWindowFactory}

/**
  * Tests input window creation
  * @author Mikko Hilpinen
  * @since 6.3.2021, v0.1
  */
object InputWindowTest extends App
{
	Changing.listenerDebuggingLimit = 100
	
	val icons = ImageCache.icons("Reach/test-images", Some(Size.square(40))).mapValues { _.map { _.cropped } }
	val selectedBoxIcon = icons("check-box-selected.png")
	val unselectedBoxIcon = icons("check-box-empty.png")
	
	object TestWindows extends InputWindowFactory[Model, Unit]
	{
		// ATTRIBUTES	-------------------------
		
		private val defaultFieldWidth = 5.cm.toScreenPixels.any
		private val fieldBackground = colors.primary.light
		
		override protected lazy val windowContext =
			ReachTestContext.windowContext.withWindowBackground(colors.primary)
		override protected lazy val contentContext = {
			val base = windowContext.against(fieldBackground)
			base.forTextComponents.mapFont { _ * 0.8 } -> base.forTextComponents.singleLine
		}
		override protected lazy val warningPopupContext =
			windowContext.borderless.nonResizable
				.withContentContext(baseContext.against(colors.failure).forTextComponents)
		
		override protected lazy val closeIcon = icons("close.png")
		
		
		// IMPLEMENTED	-------------------------
		
		override implicit protected def log: Logger = ReachTestContext.log
		
		override protected def inputTemplate = {
			val nameErrorPointer = EventfulPointer(LocalizedString.empty)
			val firstNameField = InputRowBlueprint("firstName").center.usingVariable(TextField) { fieldF =>
				val textPointer = EventfulPointer[String]("")
				val displayErrorPointer = nameErrorPointer.mergeWith(textPointer) { (error, text) =>
					if (text.isEmpty) error else LocalizedString.empty
				}
				val field = fieldF.withErrorMessagePointer(displayErrorPointer).withFieldName("First Name")
					.string(defaultFieldWidth, textPointer = textPointer)
				field.validateWith { s =>
					if (s.isEmpty) {
						nameErrorPointer.value = "Required Field"
						"Please insert a value first"
					}
					else
						LocalizedString.empty
				}
			}
			
			val lastNameField = InputRowBlueprint("lastName").center.usingVariable(TextField) {
				_.withFieldName("Last Name").withHint("Optional").string(defaultFieldWidth) }
				
			val sexField = InputRowBlueprint("isMale", "Sex", BottomLeft)
				.usingVariable(RadioButtonGroup) {
					_(Pair[(Boolean, LocalizedString)](true -> "Male", false -> "Female")) }
			
			val durationField = InputRowBlueprint("durationSeconds", "Login Duration")
				.usingVariable(DurationField) { _.withMaxValue(24.hours).apply().convertWith { _.toSeconds }
			}
			val acceptTermsField = InputRowBlueprint("accept", "I accept the terms and conditions of use",
				fieldAlignment = Alignment.Left, scalable = false)
				.using(CheckBox) {
					_.icons(Pair(unselectedBoxIcon, selectedBoxIcon)).validateWith {
						if (_) LocalizedString.empty else "You must accept the terms and conditions to continue"
					}
				}
			Vector(RowGroups(RowGroup(firstNameField, lastNameField),
				RowGroup.singleRow(sexField), RowGroup.singleRow(durationField)),
				RowGroups.singleRow(acceptTermsField)) -> ()
				
			
			// Vector() -> ()
		}
		
		override protected def buildLayout(factories: ContextualMixed[StaticTextContext],
		                                   content: Seq[OpenComponent[ReachComponent, Changing[Boolean]]],
		                                   context: Unit) =
		{
			val framingMargin = margins.medium.downscaling x margins.medium.any
			
			// Frames content
			if (content.size == 1)
				factories(Framing).withBackground(fieldBackground).withInsets(framingMargin).apply(content.head)
			// If there are many, wraps them in a stack also
			else if (content.forall { _.result.isAlwaysTrue })
				factories(Stack).build(Framing) { framingF =>
					content.map { c => framingF.withBackground(fieldBackground).withInsets(framingMargin).apply(c) }
				}
			else
				factories(ViewStack).build(Framing) { factories =>
					content.map { c =>
						factories.next().withBackground(fieldBackground).withInsets(framingMargin)(c).parentAndResult
					}
				}
		}
		
		override protected def specifyButtons(context: Unit,
		                                      input: => Either[(String, ReachComponent with FocusRequestable), Model],
		                                      warn: (String, LocalizedString) => Unit) =
		{
			val okButton = model.WindowButtonBlueprint[Model]("OK", role = Secondary, isDefault = true) { promise =>
				input.toOption.foreach(promise.trySuccess)
			}
			// NB: Correct way here would be to test that the check box doesn't have focus
			Vector(okButton) -> AlwaysTrue
		}
		
		override protected def defaultCloseButtonText = "Cancel"
		
		override protected def executionContext = exc
		
		override protected def buttonContext(buttonColor: ColorRole, hasIcon: Boolean) = {
			val base = windowContext
			if (hasIcon)
				base.withTextAlignment(Alignment.Left)
					.mapTextInsets { _.mapLeft { _ * 0.5 }.mapRight { _ * 1.5 }.expandingToRight }
					.withBackground(buttonColor)
			else
				base.withTextAlignment(Alignment.Center)
					.mapTextInsets { _.mapHorizontal { _ * 1.5 } }
					.withBackground(buttonColor)
		}
		
		override protected def defaultResult = Model.empty
		
		override protected def title = "Test"
	}
	
	val runTime = Runtime.getRuntime
	val maxMemory = runTime.maxMemory()
	def printMemoryStatus() = {
		val used = runTime.totalMemory()
		println(s"${ used * 100 / maxMemory }% (${used/100000} M) used")
	}
	
	start()
	
	// Displays a dialog
	val result = TestWindows.displayBlocking().get
	println(s"Dialog completed with result: $result")
	
	println(result("durationSeconds").getLong.seconds.description)
}
