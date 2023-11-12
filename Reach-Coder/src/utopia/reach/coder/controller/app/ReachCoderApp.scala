package utopia.reach.coder.controller.app

import utopia.coder.controller.app.{AppLogic, CoderApp}
import utopia.reach.coder.util.Common.jsonParser

/**
  * The main application class for this project / module
  * @author Mikko Hilpinen
  * @since 30.5.2023, v1.0
  */
object ReachCoderApp extends App with CoderApp
{
	override protected val logicOptions: Iterable[AppLogic] = Some(ReachCoderAppLogic)
	
	run(args.toIndexedSeq)
}
