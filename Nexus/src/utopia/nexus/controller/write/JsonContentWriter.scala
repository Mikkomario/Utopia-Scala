package utopia.nexus.controller.write

import utopia.access.model.enumeration.ContentCategory.{Application, Text}
import utopia.access.model.enumeration.Status
import utopia.access.model.enumeration.Status.OK
import utopia.access.model.{HasHeaders, Headers}
import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.mutable.DataType.ModelType
import utopia.flow.util.StringExtensions._
import utopia.nexus.controller.write.JsonContentWriter.JsonEnveloper.JsonEnvelopeNames
import utopia.nexus.controller.write.JsonContentWriter.{JsonEnveloper, PlainJsonContentWriter}
import utopia.nexus.controller.write.WriteResponseBody.NoBody
import utopia.nexus.model.request.RequestContext
import utopia.nexus.model.response.ResponseContent

object JsonContentWriter
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * Default name of the description response property
	 */
	val defaultDescriptionPropName: String = "message"
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @param naming Implicit JSON property names to use
	 * @return A content writer that writes all responses in JSON envelopes
	 */
	def enveloped(implicit naming: JsonEnvelopeNames = JsonEnvelopeNames.default) = new JsonEnveloper()
	
	
	// OTHER    ----------------------------
	
	/**
	 * Creates a content writer that supports optional enveloping
	 * @param envelopHeaderNames Names of the headers that control enveloping. Default = "X-Envelop"
	 * @param envelopParamNames Names of the (query) parameters that control enveloping. Default = "envelop".
	 * @param envelopsByDefault Whether to envelope responses by default. Default = false.
	 * @param mayWriteDescriptionAsPlainText Whether to write description only -results as plain text in
	 *                                       non-enveloped responses.
	 *                                       Default = false = descriptions will always be presented in JSON objects.
	 * @param naming Implicit property names to use
	 * @return A new content writer
	 */
	def apply(envelopHeaderNames: Iterable[String] = defaultEnvelopHeaderNames,
	          envelopParamNames: Iterable[String] = defaultEnvelopParamNames, envelopsByDefault: Boolean = false,
	          mayWriteDescriptionAsPlainText: Boolean = false)
	         (implicit naming: JsonEnvelopeNames = JsonEnvelopeNames.default) =
		new JsonContentWriter(envelopHeaderNames, envelopParamNames, envelopsByDefault,
			mayWriteDescriptionAsPlainText)
	
	/**
	 * @param descriptionPropName Name of the property containing the result's description property
	 *                            (when applicable). Default = "message".
	 * @param writeDescriptionAsPlainText Whether to write description only -results as plain text.
	 *                                    Default = false = descriptions will always be presented in JSON objects.
	 * @return A new content writer that always uses JSON
	 */
	def plain(descriptionPropName: String = defaultDescriptionPropName,
	          writeDescriptionAsPlainText: Boolean = false) =
		new PlainJsonContentWriter(descriptionPropName, writeDescriptionAsPlainText)
	
	
	// NESTED   ----------------------------
	
	object JsonEnveloper
	{
		// NESTED   ------------------------
		
		object JsonEnvelopeNames
		{
			/**
			 * The default property names to use in JSON enveloping.
			 */
			lazy val default = apply()
		}
		/**
		 * Specifies which property names to use in JSON envelopes
		 * @param value Name of the property that holds the result value. Default = "value".
		 * @param valueOnFailure Name of the property that holds the result value in case of a failure response.
		 *                       Note: Often failure responses contain a description instead of a value.
		 *                       Default = "value".
		 * @param description Name of the property that contains the result's description.
		 *                    Default = "message".
		 * @param status Name of the property that contains the response status. Default = "status".
		 * @param headers Name of the property that contains the response headers. Default = "headers".
		 */
		case class JsonEnvelopeNames(value: String = "value", valueOnFailure: String = "value",
		                             description: String = defaultDescriptionPropName, status: String = "status",
		                             headers: String = "headers")
	}
	/**
	 * A content writer that writes all responses in JSON envelopes
	 * @param naming Implicit JSON property names to use
	 */
	class JsonEnveloper()(implicit naming: JsonEnvelopeNames = JsonEnvelopeNames.default) extends ContentWriter[Any]
	{
		override def prepare(content: ResponseContent, status: Status, headers: Headers)
		                    (implicit context: Any): (WriteResponseBody, Status) =
		{
			val body = WriteResponseBody.json(Model.withConstants(OptimizedIndexedSeq.concat[Constant](
				Pair(Constant(naming.status, status.code),
					Constant(if (status.isSuccess) naming.value else naming.valueOnFailure, content.value)),
				content.description.ifNotEmpty.map { Constant(naming.description, _) },
				Single(Constant(naming.headers, headers))
			)))
			// When enveloping, the outer status is always 200
			body -> OK
		}
	}
	/**
	 * A content writer that always uses JSON
	 * @param descriptionPropName Name of the property containing the result's description property
	 *                            (when applicable). Default = "message".
	 * @param writeDescriptionAsPlainText Whether to write description only -results as plain text.
	 *                                    Default = false = descriptions will always be presented in JSON objects.
	 */
	class PlainJsonContentWriter(descriptionPropName: String = defaultDescriptionPropName,
	                             writeDescriptionAsPlainText: Boolean = false)
		extends ContentWriter[HasHeaders]
	{
		// ATTRIBUTES   --------------------
		
		// The description written based on the requested content type
		private lazy val descriptionWriter = {
			val json = Application.json -> JsonDescriptionWriter
			val text = Text.plain -> PlainTextDescriptionWriter
			DelegatingMultiTypeContentWriter(if (writeDescriptionAsPlainText) Pair(text, json) else Pair(json, text))
		}
		
		
		// IMPLEMENTED  --------------------
		
		override def prepare(content: ResponseContent, status: Status, headers: Headers)
		                    (implicit context: HasHeaders): (WriteResponseBody, Status) =
		{
			if (content.value.isEmpty) {
				// Case: Empty result => No body will be written
				if (content.description.isEmpty)
					NoBody -> status
				// Case: Description only => Uses the description writer
				else
					descriptionWriter.prepare(content, status, headers)
			}
			else {
				val body = {
					// Case: No description needs or may be written => Only writes the value
					if (descriptionPropName.isEmpty || content.description.isEmpty ||
						content.value.dataType != ModelType)
						WriteResponseBody.json(content.value)
					// Case: Value is a model and a description is included => Adds it as a separate property
					else
						WriteResponseBody.json(content.value.getModel +
							Constant(descriptionPropName, content.description))
				}
				body -> status
			}
		}
		
		
		// NESTED   -----------------------
		
		/**
		 * Writes result descriptions as plain text
		 */
		private object PlainTextDescriptionWriter extends ContentWriter[HasHeaders]
		{
			override def prepare(content: ResponseContent, status: Status, headers: Headers)
			                    (implicit context: HasHeaders): (WriteResponseBody, Status) =
				WriteResponseBody.plainText(content.description,
					context.headers.preferredCharset.getOrElse { headers.charsetOrUtf8 }) -> status
		}
		/**
		 * Writes result descriptions as JSON, possibly wrapping them in JSON objects
		 */
		private object JsonDescriptionWriter extends ContentWriter[Any]
		{
			override def prepare(content: ResponseContent, status: Status, headers: Headers)
			                    (implicit context: Any): (WriteResponseBody, Status) =
			{
				val body = descriptionPropName.ifNotEmpty match {
					case Some(propName) =>
						WriteResponseBody.json(s"{\"$propName\": ${ (content.description: Value).toJson }}")
					case None => WriteResponseBody.json(content.description: Value)
				}
				body -> status
			}
		}
	}
}
/**
 * A content writer that writes JSON and supports optional enveloping
 * @param envelopHeaderNames Names of the headers that control enveloping. Default = "X-Envelop"
 * @param envelopParamNames Names of the (query) parameters that control enveloping. Default = "envelop".
 * @param envelopsByDefault Whether to envelope responses by default. Default = false.
 * @param mayWriteDescriptionAsPlainText Whether to write description only -results as plain text in
 *                                       non-enveloped responses.
 *                                       Default = false = descriptions will always be presented in JSON objects.
 * @param naming Implicit property names to use
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.0
 */
class JsonContentWriter(override protected val envelopHeaderNames: Iterable[String] = defaultEnvelopHeaderNames,
                        override protected val envelopParamNames: Iterable[String] = defaultEnvelopParamNames,
                        override protected val envelopsByDefault: Boolean = false,
                        mayWriteDescriptionAsPlainText: Boolean = false)
                       (implicit naming: JsonEnvelopeNames = JsonEnvelopeNames.default)
	extends PossiblyEnvelopingContentWriter[RequestContext[_]]
{
	override protected lazy val envelopingDelegate: ContentWriter[RequestContext[_]] = new JsonEnveloper()
	override protected lazy val plainDelegate: ContentWriter[RequestContext[_]] =
		new PlainJsonContentWriter(naming.description, mayWriteDescriptionAsPlainText)
}
