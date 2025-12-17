package utopia.echo.model.request.tts.piper

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.controller.parse.ResponseParser
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import java.nio.file.Path
import scala.concurrent.Future

object TextToAudioFileRequest
{
	/**
	 * Creates a new request for converting text into a .wav audio file
	 * @param text Text to convert
	 * @param path Path to the .wav file to write. Call-by-name, called when receiving a response.
	 * @param deprecationView A view that will contain true if this request should be retracted (unless already sent).
	 *                        Default = always false, which is the appropriate value for almost all local requests.
	 * @param params Implicit customization parameters (default = empty = no customization)
	 * @param log Implicit logging implementation used for recording failures to parse failure response bodies.
	 * @return A new request
	 */
	def apply(text: String, path: => Path, deprecationView: View[Boolean] = AlwaysFalse)
	         (implicit params: TtsParams = TtsParams.empty, log: Logger) =
		new TextToAudioFileRequest(text, params, path, deprecationView)
}

/**
 * A Piper TTS request for converting text to an .wav audio file.
 * @author Mikko Hilpinen
 * @since 28.09.2025, v1.4
 */
class TextToAudioFileRequest(override val text: String, override val params: TtsParams, path: => Path,
                             deprecationView: View[Boolean])
                            (implicit log: Logger)
	extends TextToSpeechRequest[Path]
{
	// IMPLEMENTED  -----------------------
	
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Path]] =
		prepared.send(ResponseParser.write.to(path).unwrapToResponse { _.toJson }.withResponseBodyAsFailureMessage)
}
