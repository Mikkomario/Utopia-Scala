package utopia.reflection.test.swing

import utopia.firmament.image.ButtonImageSet
import utopia.flow.time.WeekDays
import utopia.flow.time.WeekDays.MondayToSunday
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.swing.input.{Calendar, JDropDownWrapper}
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.layout.wrapper.Framing
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.stack.LengthExtensions._
import utopia.reflection.shape.stack.{StackInsets, StackLength}
import utopia.reflection.test.TestContext._
import utopia.genesis.text.FontStyle.Plain

import java.nio.file.Paths
import java.time.{Month, Year}

/**
  * Tests calendar component visually
  * @author Mikko Hilpinen
  * @since 3.8.2019, v1+
  */
object CalendarTest extends App
{
	implicit val weekdays: WeekDays = MondayToSunday
	ParadigmDataType.setup()
	
	val basicFont = Font("Arial", 14, Plain, 2)
	val smallFont = basicFont * 0.75
	
	val yearSelect = new JDropDownWrapper[Year](StackInsets.symmetric(16.any, 4.upscaling), "Year", basicFont,
		Color.white, Color.magenta, initialContent = (1999 to 2050).map { Year.of }.toVector)
	val monthSelect = new JDropDownWrapper[Month](StackInsets.symmetric(16.any, 4.upscaling), "Month", basicFont,
		Color.white, Color.magenta, initialContent = Month.values().toVector)
	
	val buttonImage = Image.readFrom(Paths.get("Reflection/test-images/arrow-back-48dp.png")).get
	val backImages = ButtonImageSet.varyingAlpha(buttonImage, 0.66, 1)
	val forwardImages = ButtonImageSet.varyingAlpha(buttonImage.flippedHorizontally, 0.66, 1)
	
	val calendar = Calendar(monthSelect, yearSelect, forwardImages, backImages, 8.any, StackLength(0, 8, 16),
		DisplayFunction.raw, smallFont,
		Color.textBlack, StackInsets.symmetric(4.upscaling, 8.upscaling), smallFont, Color.textBlack,
		StackInsets.symmetric(4.upscaling, 6.upscaling), Color.black.withAlpha(0.33), Color.cyan)
	
	calendar.valuePointer.addContinuousListener { e => println(s"New selected date: ${ e.newValue }") }
	
	// Creates the frame and displays it
	val actorHandler = ActorHandler()
	val actionLoop = new ActorLoop(actorHandler)
	
	val framing = Framing.symmetric(calendar, 24.downscaling.square)
	framing.background = Color.white
	val frame = Frame.windowed(framing, "Calendar Test", User)
	frame.setToExitOnClose()
	
	println(s"Final connection status (from Frame): ${ frame.attachmentDescription }")
	
	actionLoop.runAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.visible = true
}
