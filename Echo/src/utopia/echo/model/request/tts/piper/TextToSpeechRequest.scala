package utopia.echo.model.request.tts.piper

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.annex.controller.ApiClient.PreparedRequest
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.model.request.Body
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future

object TextToSpeechRequest
{
	// OTHER    ----------------------------
	
	/**
	 * Creates a new text-to-speech request
	 * @param text Text to convert into speech
	 * @param deprecationView A view that contains true if this request should be retracted (unless already sent).
	 *                        Default = always false, which should pretty much always be the case when dealing
	 *                        with a local server.
	 * @param send A function that receives a prepared request and sends it,
	 *             specifying how the response will be processed.
	 *             Should be prepared to handle wav audio data (even if the response headers say otherwise).
	 * @param params Implicit parameters used for customizing the text-to-speech process.
	 *               Default = no customization.
	 * @tparam A Type of the parsed results.
	 * @return A new request.
	 */
	def apply[A](text: String, deprecationView: View[Boolean] = AlwaysFalse)
	            (send: PreparedRequest => Future[RequestResult[A]])
	            (implicit params: TtsParams = TtsParams.empty): TextToSpeechRequest[A] =
		new _TextToSpeechRequest[A](text, params, deprecationView, send)
	
	/**
	 * Creates a factory for constructing text-to-speech requests
	 * @param send A function that receives a prepared request and sends it,
	 *             specifying how the response will be processed.
	 *             Should be prepared to handle wav audio data (even if the response headers say otherwise).
	 * @tparam A Type of the parsed results.
	 * @return A new request factory.
	 */
	def factory[A](send: PreparedRequest => Future[RequestResult[A]]) = new TtsRequestFactory[A](send)
	
	
	// NESTED   ----------------------------
	
	class TtsRequestFactory[+A](send: PreparedRequest => Future[RequestResult[A]])
	{
		/**
		 * Creates a new text-to-speech request
		 * @param text Text to convert into speech
		 * @param deprecationView A view that contains true if this request should be retracted (unless already sent).
		 *                        Default = always false, which should pretty much always be the case when dealing
		 *                        with a local server.
		 * @param params Implicit parameters used for customizing the text-to-speech process.
		 *               Default = no customization.
		 * @return A new request.
		 */
		def apply(text: String, deprecationView: View[Boolean] = AlwaysFalse)
		         (implicit params: TtsParams = TtsParams.empty) =
			TextToSpeechRequest(text, deprecationView)(send)
	}
	
	private class _TextToSpeechRequest[+A](override val text: String, override val params: TtsParams,
	                                       deprecationView: View[Boolean],
	                                       f: PreparedRequest => Future[RequestResult[A]])
		extends TextToSpeechRequest[A]
	{
		override def deprecated: Boolean = deprecationView.value
		
		override def send(prepared: PreparedRequest): Future[RequestResult[A]] = f(prepared)
	}
}

/**
 * A local Piper API -compatible request, calling the text-to-speech functionality.
 * @author Mikko Hilpinen
 * @since 28.09.2025, v1.4
 */
trait TextToSpeechRequest[+A] extends ApiRequest[A]
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return Text to convert into speech
	 */
	def text: String
	/**
	 * @return Parameters for customizing the speech output
	 */
	def params: TtsParams
	
	
	// IMPLEMENTED  ------------------------
	
	override def method: Method = Post
	override def path: String = ""
	override def pathParams: Model = Model.empty
	
	override def body: Either[Value, Body] = Left(Constant("text", text) +: params.toModel)
}
