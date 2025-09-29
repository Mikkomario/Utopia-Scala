package utopia.echo.test

import utopia.access.model.Headers
import utopia.access.model.enumeration.ContentCategory.Application
import utopia.access.model.enumeration.Method.Post
import utopia.disciple.controller.parse.ResponseParser
import utopia.disciple.model.error.RequestFailedException
import utopia.disciple.model.request.{Request, StringBody}
import utopia.echo.test.EchoTestContext._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.string.StringFrom
import utopia.flow.util.TryExtensions._

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import scala.util.Failure

/**
 * Tests generating
 * @author Mikko Hilpinen
 * @since 28.09.2025, v1.4
 */
object PiperTest extends App
{
	println("Performing a request")
	// Note: The response headers are lying; The content is really a wav file.
	private val result = gateway
		.makeBlockingRequest(Request("http://localhost:5000", method = Post,
			body = Some(StringBody.json(Model.from("text" -> "Tämä on toinen testilause.").toJson)),
			headers = Headers.withContentType(Application.json))) { response =>
			response.consume(ResponseParser.blocking { (status, headers, stream) =>
				println(s"$status: $headers")
				if (status.isSuccess) {
					println("Successful response")
					println("Successful response; Writing the response body to a file.")
					stream match {
						case Some(stream) =>
							Paths.get("Echo/data/test-output/test.wav")
								.createParentDirectories()
								.flatMap { _.writeStream(stream) }
						case None =>
							println("No content")
							Failure(new IllegalStateException("Empty response"))
					}
				}
				else {
					println("Request failed")
					val body = stream match {
						case Some(stream) =>
							StringFrom.stream(stream, headers.charset.getOrElse(StandardCharsets.UTF_8))
								.getOrMap { error =>
									log(error)
									s"<Parse error: ${ error.getMessage }>"
								}
						case None =>
							println("Empty response")
							"<empty>"
					}
					Failure(new RequestFailedException(s"Server responded with $status: $body"))
				}
			})
		}
	
	private val path = result.get.wrapped.get
	println(s"Generated $path")
	path.openDirectory()
	
	println("Done!")
}
