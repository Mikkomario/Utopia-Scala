package utopia.reflection.test

import utopia.firmament.context.{AnimationContext, BaseContext, ScrollingContext}
import utopia.firmament.localization.{Localizer, NoLocalization}
import utopia.firmament.model.Margins
import utopia.flow.async.context.ThreadPool
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.event.keyboard.KeyboardEvents
import utopia.genesis.text.Font
import utopia.genesis.text.FontStyle.Plain
import utopia.paradigm.color.{ColorScheme, ColorSet}
import utopia.paradigm.generic.ParadigmDataType

import scala.concurrent.ExecutionContext

/**
  * A set of context definitions for Utopia tests
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
  */
object TestContext
{
	ParadigmDataType.setup()
	
	val actorHandler = ActorHandler()
	val colorScheme = ColorScheme.default ++
		ColorScheme.twoTone(ColorSet.fromHexes("#212121", "#484848", "#000000").get,
			ColorSet.fromHexes("#ffab00", "#ffdd4b", "#c67c00").get)
	val font = Font("Arial", 12, Plain, 2)
	val margins = Margins(12)
	
	implicit val localizer: Localizer = NoLocalization
	val baseContext: BaseContext = BaseContext(actorHandler, font, colorScheme, margins)
	
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Reflection")
	KeyboardEvents.specifyExecutionContext(exc)
	implicit val animationContext: AnimationContext = AnimationContext(actorHandler)
	implicit val scrollingContext: ScrollingContext = ScrollingContext.withDarkRoundedBar(actorHandler)
	
	implicit val defaultLanguageCode: String = "EN"
	
}
