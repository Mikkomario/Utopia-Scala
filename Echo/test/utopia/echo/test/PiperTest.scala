package utopia.echo.test

import utopia.annex.model.response.{RequestFailure, Response}
import utopia.annex.util.RequestResultExtensions._
import utopia.echo.controller.client.PiperClient
import utopia.echo.model.request.tts.piper.TextToAudioFileRequest
import utopia.echo.test.EchoTestContext._
import utopia.flow.parse.file.FileExtensions._

import java.nio.file.Path

/**
 * Tests generating
 * @author Mikko Hilpinen
 * @since 28.09.2025, v1.4
 */
object PiperTest extends App
{
	println("Performing a request")
	PiperClient().push(TextToAudioFileRequest("Tämä on toinen testilause", "Echo/data/test-output/test.wav"))
		.future.waitForResult() match
	{
		case Response.Success(path: Path, _, _) =>
			println(s"Generated ${ path.toJson }")
			path.openDirectory()
			
		case failure: RequestFailure => log(failure.cause)
	}
	
	println("Done!")
}
