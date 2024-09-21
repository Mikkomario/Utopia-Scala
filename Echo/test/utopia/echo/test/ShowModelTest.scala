package utopia.echo.test

import EchoTestContext._
import utopia.annex.util.RequestResultExtensions._
import utopia.flow.collection.CollectionExtensions._

/**
  * Tests the show command
  * @author Mikko Hilpinen
  * @since 20.09.2024, v1.1
  */
object ShowModelTest extends App
{
	selectModel().foreach { implicit llm =>
		client.showModel.future.waitForResult().toTry.logToOption.foreach { modelInfo =>
			println(modelInfo)
			println(s"\n\nParsed system message: \"${ modelInfo.systemMessage }\"")
		}
	}
}
