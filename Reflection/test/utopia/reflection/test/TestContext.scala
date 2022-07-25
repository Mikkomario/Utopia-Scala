package utopia.reflection.test

import utopia.flow.async.ThreadPool
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.reflection.color.{ColorScheme, ColorSet}
import utopia.reflection.component.context.{AnimationContext, BaseContext, ScrollingContext}
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.Margins
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain

import scala.concurrent.ExecutionContext

/**
  * A set of context definitions for Utopia tests
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
  */
object TestContext
{
	GenesisDataType.setup()
	
	val actorHandler = ActorHandler()
	val colorScheme = ColorScheme.twoTone(ColorSet.fromHexes("#212121", "#484848", "#000000").get,
		ColorSet.fromHexes("#ffab00", "#ffdd4b", "#c67c00").get)
	val font = Font("Arial", 12, Plain, 2)
	val margins = Margins(12)
	
	val baseContext: BaseContext = BaseContext(actorHandler, font, colorScheme, margins)
	
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Reflection").executionContext
	GlobalKeyboardEventHandler.specifyExecutionContext(exc)
	implicit val animationContext: AnimationContext = AnimationContext(actorHandler)
	implicit val scrollingContext: ScrollingContext = ScrollingContext.withDarkRoundedBar(actorHandler)
	
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
}
