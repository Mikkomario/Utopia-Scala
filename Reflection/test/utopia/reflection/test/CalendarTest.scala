package utopia.reflection.test

import java.nio.file.Paths
import java.time.format.TextStyle
import java.time.{DayOfWeek, Month, Year}
import java.util.Locale

import utopia.flow.async.ThreadPool
import utopia.reflection.shape.LengthExtensions._
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.reflection.component.swing.{Calendar, JDropDownWrapper}
import utopia.reflection.component.swing.button.ButtonImageSet
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.Framing
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.localization.{DisplayFunction, Localizer, NoLocalization}
import utopia.reflection.shape.{StackInsets, StackLength}
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain

import scala.concurrent.ExecutionContext

/**
  * Tests calendar component visually
  * @author Mikko Hilpinen
  * @since 3.8.2019, v1+
  */
object CalendarTest extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	val basicFont = Font("Arial", 14, Plain, 2)
	val smallFont = basicFont * 0.75
	
	val yearSelect = new JDropDownWrapper[Year](StackInsets.symmetric(16.any, 4.upscaling), "Year", basicFont,
		Color.white, Color.magenta, initialContent = (1999 to 2050).map {Year.of}.toVector)
	val monthSelect = new JDropDownWrapper[Month](StackInsets.symmetric(16.any, 4.upscaling), "Month", basicFont,
		Color.white, Color.magenta, initialContent = Month.values().toVector)
	
	val buttonImage = Image.readFrom(Paths.get("test-images/arrow-back-48dp.png")).get
	val backImages = ButtonImageSet.varyingAlpha(buttonImage, 0.66, 1)
	val forwardImages = ButtonImageSet.varyingAlpha(buttonImage.flippedHorizontally, 0.66, 1)
	
	val calendar = Calendar(monthSelect, yearSelect, forwardImages, backImages, 8.any, StackLength(0, 8, 16),
		DisplayFunction.noLocalization[DayOfWeek] { _.getDisplayName(TextStyle.SHORT, Locale.getDefault) }, smallFont,
		Color.textBlack, StackInsets.symmetric(4.upscaling, 8.upscaling), smallFont, Color.textBlack,
		StackInsets.symmetric(4.upscaling, 6.upscaling) , Color.black.withAlpha(0.33), Color.cyan)
	
	calendar.addValueListener { e => println(s"New selected date: ${e.newValue}") }
	
	// Creates the frame and displays it
	val actorHandler = ActorHandler()
	val actionLoop = new ActorLoop(actorHandler)
	implicit val context: ExecutionContext = new ThreadPool("Reflection").executionContext
	
	val framing = Framing.symmetric(calendar, 24.downscaling.square)
	framing.background = Color.white
	val frame = Frame.windowed(framing, "Calendar Test", User)
	frame.setToExitOnClose()
	
	println(s"Final connection status (from Frame): ${frame.attachmentDescription}")
	
	actionLoop.registerToStopOnceJVMCloses()
	actionLoop.startAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.isVisible = true
}
