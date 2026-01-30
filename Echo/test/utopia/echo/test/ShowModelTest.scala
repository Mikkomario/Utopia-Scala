package utopia.echo.test

import utopia.annex.util.RequestResultExtensions._
import utopia.echo.test.EchoTestContext._
import utopia.flow.util.result.TryExtensions._

/**
  * Tests the show command
  * @author Mikko Hilpinen
  * @since 20.09.2024, v1.1
  */
object ShowModelTest extends App
{
	selectModel().foreach { implicit llm =>
		ollamaClient.showModel.future.waitForResult().toTry.log.foreach { modelInfo =>
			println(modelInfo)
			println(s"\n\nParsed system message: \"${ modelInfo.systemMessage }\"")
		}
	}
}
