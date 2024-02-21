package utopia.reach.test

import utopia.firmament.context.{AnimationContext, BaseContext, ScrollingContext, WindowContext}
import utopia.firmament.localization.{Localizer, NoLocalization}
import utopia.firmament.model.Margins
import utopia.firmament.model.enumeration.WindowResizePolicy.UserAndProgram
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.genesis.handling.action.{ActionLoop, ActorHandler}
import utopia.genesis.handling.event.keyboard.KeyboardEvents
import utopia.genesis.text.Font
import utopia.genesis.text.FontStyle.Plain
import utopia.genesis.util.{Fps, Screen}
import utopia.paradigm.color.{ColorScheme, ColorSet}
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.measurement.DistanceExtensions._
import utopia.paradigm.measurement.Ppi
import utopia.paradigm.transform.Adjustment
import utopia.reach.context.{ReachContentWindowContext, ReachWindowContext}

import scala.concurrent.ExecutionContext

/**
  * Contains common context items used when performing Reach-specific tests.
  * @author Mikko Hilpinen
  * @since 16.4.2023, v1.0
  */
object ReachTestContext
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	ParadigmDataType.setup()
	
	// ATTRIBUTES   ------------------------
	
	implicit val log: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Reach")
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	implicit val adjustment: Adjustment = Adjustment(0.25)
	
	implicit val ppi: Ppi = Screen.ppi
	val cm = 1.cm.toPixels.round.toInt
	
	val actorHandler = ActorHandler()
	implicit val animationContext: AnimationContext = AnimationContext(actorHandler)
	implicit val scrollingContext: ScrollingContext = ScrollingContext.withDarkRoundedBar(actorHandler)
	
	val colors = ColorScheme.default ++
		ColorScheme.twoTone(
			ColorSet.fromHexes("#212121", "#484848", "#000000").get,
			ColorSet.fromHexes("#ffab00", "#ffdd4b", "#c67c00").get
		)
	val font = Font("Arial", (cm * 0.75).round.toInt, Plain)
	val margins = Margins((cm * 0.5).round.toInt)
	val baseContext: BaseContext = BaseContext(actorHandler, font, colors, margins)
	implicit val windowContext: ReachContentWindowContext = ReachWindowContext(
		WindowContext(actorHandler), colors.primary.light)
		.withResizeLogic(UserAndProgram).withCursors(TestCursors.cursors)
		.withContentContext(baseContext)
	
	private val actionLoop = new ActionLoop(actorHandler, NumericSpan(5, 60).mapTo(Fps.apply))
	
	
	// INIITAL CODE -----------------------
	
	KeyboardEvents.specifyExecutionContext(exc)
	
	
	// OTHER    --------------------------
	
	def start() = actionLoop.runAsync()
}
