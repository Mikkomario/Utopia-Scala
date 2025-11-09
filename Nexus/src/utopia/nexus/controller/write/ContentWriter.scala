package utopia.nexus.controller.write

import utopia.access.model.enumeration.ContentCategory.{Application, Text}
import utopia.access.model.enumeration.Status
import utopia.access.model.enumeration.Status.OK
import utopia.access.model.{ContentType, HasHeaders, Headers}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.mutable.DataType.{ModelType, PairType, StringType, VectorType}
import utopia.flow.parse.xml.{Namespace, NamespacedString, XmlElement}
import utopia.flow.util.StringExtensions._
import utopia.nexus.controller.write.ContentWriter.JsonContentWriter.{JsonEnveloper, PlainJsonContentWriter}
import utopia.nexus.controller.write.ContentWriter.JsonContentWriter.JsonEnveloper.JsonEnvelopeNames
import utopia.nexus.controller.write.ContentWriter.JsonOrXmlContentWriter.{JsonOrXmlEnveloper, PlainJsonOrXmlContentWriter}
import utopia.nexus.controller.write.ContentWriter.XmlContentWriter.XmlEnveloper.XmlEnvelopeNames
import utopia.nexus.controller.write.ContentWriter.XmlContentWriter.{PlainXmlContentWriter, XmlElementNames, XmlEnveloper}
import utopia.nexus.controller.write.WriteResponseBody.NoBody
import utopia.nexus.http.Response
import utopia.nexus.model.request.RequestContext
import utopia.nexus.model.response.ResponseContent

object ContentWriter
{
	// ATTRIBUTES   -----------------------
	
	private lazy val defaultDescriptionPropName: String = "message"
	private lazy val defaultEnvelopHeaderNames = Single("X-Envelop")
	private lazy val defaultEnvelopParamNames: Iterable[String] = Single("envelop")
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return Access to JSON-based content writer constructors
	 */
	def json = JsonContentWriter
	/**
	 * @return Access to XML-based content writer constructors
	 */
	def xml = XmlContentWriter
	/**
	 * @return Access to constructors for content writers that support both JSON and XML
	 */
	def jsonOrXml = JsonOrXmlContentWriter
	
	
	// OTHER    --------------------------
	
	/**
	 * Converts a value into XML
	 * @param elementName Name of the generated XML element
	 * @param value Value to populate the element with
	 * @param naming Implicit naming logic
	 * @return An XML element which contains the specified value
	 */
	private def toXml(elementName: NamespacedString, value: Value)(implicit naming: XmlElementNames): XmlElement = {
		implicit val namespace: Namespace = naming.namespace
		// Case: Empty value => Empty XML element
		if (value.isEmpty)
			XmlElement(elementName)
		else
			value.dataType match {
				// Case: Model => Converts into an XML element with named children
				case ModelType => toXml(elementName, value.getModel)
				// Case: Collection => Adds n list item elements as children
				case VectorType | PairType =>
					val coll = value.getVector
					XmlElement(elementName,
						attributeMap = Map(namespace -> Model.from(naming.listLengthAttribute -> coll.size)),
						children = coll.map { toXml(naming.listItem, _) })
				case _ =>
					value.castTo(ModelType, StringType) match {
						// Case: Converts to model
						case Left(modelVal) => toXml(elementName, modelVal.getModel)
						// Case: Other type => Stores the value as a simple string value inside the XML element
						case Right(strVal) => XmlElement(elementName, strVal)
					}
			}
	}
	/**
	 * Converts a model into an XML element
	 * @param elementName Name of the generated element
	 * @param model Model to convert into XML
	 * @param naming Implicit naming logic
	 * @return An XML element containing all information from the specified model
	 */
	private def toXml(elementName: NamespacedString, model: Model)(implicit naming: XmlElementNames): XmlElement =
		XmlElement(elementName,
			children = model.propertiesIterator.map { p => toXml(p.name.capitalize, p.value) }.toOptimizedSeq)
			
	
	// NESTED   ----------------------------
	
	object DelegatingMultiTypeContentWriter
	{
		// OTHER    ------------------------
		
		/**
		 * Creates a new delegating content writer
		 * @param delegates The delegates used for performing the actual writing, from most to least preferred.
		 *                  Each entry contains:
		 *                      1. The content type handled by the specified writer
		 *                      1. The writer for that content type
		 * @tparam C Type of required context
		 * @return A new content writer that delegates the writing to one of the specified delegates
		 */
		def apply[C <: HasHeaders](delegates: Iterable[(ContentType, ContentWriter[C])]): DelegatingMultiTypeContentWriter[C] =
			_DelegatingMultiTypeContentWriter(delegates)
		
		
		// NESTED   ------------------------
		
		private case class _DelegatingMultiTypeContentWriter[-C <: HasHeaders](delegates: Iterable[(ContentType, ContentWriter[C])])
			extends DelegatingMultiTypeContentWriter[C]
	}
	/**
	 * Common trait for content writers which delegate the writing to other writers, based on the desired content type
	 * @tparam C Required contextual information
	 */
	trait DelegatingMultiTypeContentWriter[-C <: HasHeaders] extends ContentWriter[C]
	{
		// ABSTRACT ------------------------
		
		/**
		 * @return The delegates used for performing the actual writing, from most to least preferred.
		 *         Each entry contains:
		 *              1. The content type handled by the specified writer
		 *              1. The writer for that content type
		 */
		protected def delegates: Iterable[(ContentType, ContentWriter[C])]
		
		
		// COMPUTED ------------------------
		
		private def defaultDelegate = delegates.head._2
		
		
		// IMPLEMENTED  --------------------
		
		override def prepare(content: ResponseContent, status: Status, headers: Headers)
		                    (implicit context: C): (WriteResponseBody, Status) =
		{
			// Finds the delegate to use for this result / request
			// Option 1: Content type is specified in response headers already
			val delegate = headers.contentType
				.flatMap { cType => delegates.find { case (out, _) => out.overlapsWith(cType) }.map { _._2 } }
				.getOrElse {
					val accepted = context.headers.acceptedTypes
					// Case: No content type specified => Uses the default delegate
					if (accepted.isEmpty)
						defaultDelegate
					// Case: Accepted content types specified => Looks for a suitable delegate, or uses the default
					else
						delegates.find { case (cType, _) => accepted.exists { _.overlapsWith(cType) } } match {
							case Some((_, delegate)) => delegate
							case None => defaultDelegate
						}
				}
			delegate.prepare(content, status, headers)
		}
	}
	
	object PossiblyEnvelopingContentWriter
	{
		// OTHER    ---------------------------
		
		/**
		 * @param envelopingDelegate The delegate content writer that envelops the output
		 * @param plainDelegate The delegate content writer that doesn't envelop the output
		 * @param envelopHeaderNames Names of the headers that control enveloping. Default = "X-Envelop"
		 * @param envelopParamNames Names of the (query) parameters that control enveloping. Default = "envelop".
		 * @param envelopsByDefault Whether to envelope responses by default. Default = false.
		 * @tparam C Type of the required context
		 * @return A new content writer
		 */
		def apply[C <: RequestContext[_]](envelopingDelegate: ContentWriter[C], plainDelegate: ContentWriter[C],
		                                  envelopHeaderNames: Iterable[String] = defaultEnvelopHeaderNames,
		                                  envelopParamNames: Iterable[String] = defaultEnvelopParamNames,
		                                  envelopsByDefault: Boolean = false): PossiblyEnvelopingContentWriter[C] =
			_PossiblyEnvelopingContentWriter[C](envelopingDelegate, plainDelegate, envelopHeaderNames,
				envelopParamNames, envelopsByDefault)
		
		
		// NESTED   ---------------------------
		
		private case class _PossiblyEnvelopingContentWriter[-C <: RequestContext[_]](envelopingDelegate: ContentWriter[C],
		                                                                             plainDelegate: ContentWriter[C],
		                                                                             envelopHeaderNames: Iterable[String],
		                                                                             envelopParamNames: Iterable[String],
		                                                                             envelopsByDefault: Boolean)
			extends PossiblyEnvelopingContentWriter[C]
	}
	/**
	 * Common trait for content writers that support optional enveloping
	 * @tparam C Required contextual information
	 */
	trait PossiblyEnvelopingContentWriter[-C <: RequestContext[_]] extends ContentWriter[C]
	{
		// ABSTRACT ---------------------------
		
		/**
		 * @return Names of the headers that control enveloping
		 */
		protected def envelopHeaderNames: Iterable[String]
		/**
		 * @return Names of the (query) parameters that control enveloping
		 */
		protected def envelopParamNames: Iterable[String]
		
		/**
		 * @return Whether to envelope responses by default
		 */
		protected def envelopsByDefault: Boolean
		
		/**
		 * @return The delegate content writer that envelops the output
		 */
		protected def envelopingDelegate: ContentWriter[C]
		/**
		 * @return The delegate content writer that doesn't envelop the output
		 */
		protected def plainDelegate: ContentWriter[C]
		
		
		// IMPLEMENTED  -----------------------
		
		override def prepare(content: ResponseContent, status: Status, headers: Headers)
		                    (implicit context: C): (WriteResponseBody, Status) =
		{
			// Determines whether enveloping should be applied
			val shouldEnvelop = test(context.headers).getOrElse {
				val params = context.request.parameters
				envelopParamNames.findMap { params(_).boolean }.getOrElse { test(headers).getOrElse(envelopsByDefault) }
			}
			// Uses the appropriate delegate
			if (shouldEnvelop)
				envelopingDelegate.prepare(content, status, headers)
			else
				plainDelegate.prepare(content, status, headers)
		}
		
		
		// OTHER    ------------------------
		
		private def test(headers: Headers) =
			envelopHeaderNames.findMap { headers.get(_).flatMap { _.boolean } }
	}
	
	object JsonOrXmlContentWriter
	{
		// OTHER    ---------------------------
		
		/**
		 * Creates a content writer that supports both JSON and XML, with optional enveloping support
		 * @param envelopHeaderNames Names of the headers that control enveloping. Default = "X-Envelop"
		 * @param envelopParamNames Names of the (query) parameters that control enveloping. Default = "envelop".
		 * @param preferXml Whether to default to XML responses when no preference is specified
		 *                  (default = false = defaults to JSON)
		 * @param envelopsByDefault Whether to envelope responses by default. Default = false.
		 * @param noEmptyElementsInXmlEnvelope Whether to exclude empty XML elements from the XML envelope.
		 *                                     Default = false = The elements are always present, but may be empty.
		 * @param descriptionMayBePlainText Whether to write description only -results as plain text,
		 *                                  in non-enveloped JSON responses.
		 *                                  Default = false = descriptions will always be presented in JSON objects.
		 * @param jsonNaming Property names used in JSON
		 * @param xmlNaming Element names used in XML
		 * @return A new content writer
		 */
		def apply(envelopHeaderNames: Iterable[String] = defaultEnvelopHeaderNames,
		          envelopParamNames: Iterable[String] = defaultEnvelopParamNames, preferXml: Boolean = false,
		          envelopsByDefault: Boolean = false, noEmptyElementsInXmlEnvelope: Boolean = false,
		          descriptionMayBePlainText: Boolean = false)
		         (implicit jsonNaming: JsonEnvelopeNames = JsonEnvelopeNames.default,
		          xmlNaming: XmlEnvelopeNames = XmlEnvelopeNames.default) =
			new JsonOrXmlContentWriter(envelopHeaderNames, envelopParamNames, preferXml,
				envelopsByDefault, noEmptyElementsInXmlEnvelope, descriptionMayBePlainText)
		
		/**
		 * Creates a new content writer that always applies enveloping, supporting both JSON and XML.
		 * @param preferXml Whether to default to XML responses when no preference is specified
		 *                  (default = false = defaults to JSON)
		 * @param noEmptyXmlElements Whether to exclude empty XML elements from the XML responses.
		 *                           Default = false = The elements are always present, but may be empty.
		 * @param jsonNaming Property names used in JSON
		 * @param xmlNaming Element names used in XML
		 * @return A new content writer
		 */
		def enveloper(preferXml: Boolean = false, noEmptyXmlElements: Boolean = false)
		             (implicit jsonNaming: JsonEnvelopeNames = JsonEnvelopeNames.default,
		              xmlNaming: XmlEnvelopeNames = XmlEnvelopeNames.default) =
			new JsonOrXmlEnveloper(preferXml, noEmptyXmlElements)
		/**
		 * Creates a new content writer that supports both JSON and XML
		 * @param descriptionPropName Name of the property containing the result's description property
		 *                            (when applicable). Default = "message".
		 * @param preferXml Whether to default to XML responses when no preference is specified
		 *                  (default = false = defaults to JSON)
		 * @param descriptionMayBePlainText Whether to write description only -results as plain text in JSON responses.
		 *                                  Default = false = descriptions will always be presented in JSON objects.
		 * @param xmlNaming Element names used in XML
		 * @return A new content writer
		 */
		def plain(descriptionPropName: String = defaultDescriptionPropName, preferXml: Boolean = false,
		          descriptionMayBePlainText: Boolean = false)
		         (implicit xmlNaming: XmlElementNames = XmlElementNames.default) =
			new PlainJsonOrXmlContentWriter(descriptionPropName, preferXml, descriptionMayBePlainText)
		
		
		// NESTED   ---------------------------
		
		/**
		 * A content writer that always applies enveloping, supporting both JSON and XML.
		 * @param preferXml Whether to default to XML responses when no preference is specified
		 *                  (default = false = defaults to JSON)
		 * @param noEmptyXmlElements Whether to exclude empty XML elements from the XML responses.
		 *                           Default = false = The elements are always present, but may be empty.
		 * @param jsonNaming Property names used in JSON
		 * @param xmlNaming Element names used in XML
		 */
		class JsonOrXmlEnveloper(preferXml: Boolean = false, noEmptyXmlElements: Boolean = false)
		                        (implicit jsonNaming: JsonEnvelopeNames = JsonEnvelopeNames.default,
		                         xmlNaming: XmlEnvelopeNames = XmlEnvelopeNames.default)
			extends DelegatingMultiTypeContentWriter[HasHeaders]
		{
			override protected val delegates: Iterable[(ContentType, ContentWriter[HasHeaders])] = {
				val json = Application.json -> new JsonEnveloper()
				val xml = Application.xml -> new XmlEnveloper(noEmptyXmlElements)
				if (preferXml) Pair(xml, json) else Pair(json, xml)
			}
		}
		/**
		 * A content writer that supports both JSON and XML
		 * @param descriptionPropName Name of the property containing the result's description property
		 *                            (when applicable). Default = "message".
		 * @param preferXml Whether to default to XML responses when no preference is specified
		 *                  (default = false = defaults to JSON)
		 * @param descriptionMayBePlainText Whether to write description only -results as plain text in JSON responses.
		 *                                  Default = false = descriptions will always be presented in JSON objects.
		 * @param xmlNaming Element names used in XML
		 */
		class PlainJsonOrXmlContentWriter(descriptionPropName: String = defaultDescriptionPropName,
		                                  preferXml: Boolean = false, descriptionMayBePlainText: Boolean = false)
		                                 (implicit xmlNaming: XmlElementNames = XmlElementNames.default)
			extends DelegatingMultiTypeContentWriter[HasHeaders]
		{
			// ATTRIBUTES   --------------------
			
			override protected val delegates: Iterable[(ContentType, ContentWriter[HasHeaders])] = {
				val jsonWriter = new PlainJsonContentWriter(descriptionPropName, descriptionMayBePlainText)
				val json = Application.json -> jsonWriter
				val xml = Application.xml -> new PlainXmlContentWriter()
				val text = Text.plain -> jsonWriter
				
				if (preferXml) Vector(xml, json, text) else Vector(json, xml, text)
			}
		}
	}
	/**
	 * A content writer that supports both JSON and XML, with optional enveloping support
	 * @param envelopHeaderNames Names of the headers that control enveloping. Default = "X-Envelop"
	 * @param envelopParamNames Names of the (query) parameters that control enveloping. Default = "envelop".
	 * @param preferXml Whether to default to XML responses when no preference is specified
	 *                  (default = false = defaults to JSON)
	 * @param envelopsByDefault Whether to envelope responses by default. Default = false.
	 * @param noEmptyElementsInXmlEnvelope Whether to exclude empty XML elements from the XML envelope.
	 * @param descriptionMayBePlainText Whether to write description only -results as plain text,
	 *                                  in non-enveloped JSON responses.
	 *                                  Default = false = descriptions will always be presented in JSON objects.
	 * @param jsonNaming Property names used in JSON
	 * @param xmlNaming Element names used in XML
	 */
	class JsonOrXmlContentWriter(override protected val envelopHeaderNames: Iterable[String] = defaultEnvelopHeaderNames,
	                             override protected val envelopParamNames: Iterable[String] = defaultEnvelopParamNames,
	                             preferXml: Boolean = false, override protected val envelopsByDefault: Boolean = false,
	                             noEmptyElementsInXmlEnvelope: Boolean = false,
	                             descriptionMayBePlainText: Boolean = false)
	                            (implicit jsonNaming: JsonEnvelopeNames = JsonEnvelopeNames.default,
	                             xmlNaming: XmlEnvelopeNames = XmlEnvelopeNames.default)
		extends PossiblyEnvelopingContentWriter[RequestContext[_]]
	{
		override protected lazy val envelopingDelegate: ContentWriter[RequestContext[_]] =
			new JsonOrXmlEnveloper(preferXml, noEmptyElementsInXmlEnvelope)
		override protected lazy val plainDelegate: ContentWriter[RequestContext[_]] =
			new PlainJsonOrXmlContentWriter(jsonNaming.description, preferXml, descriptionMayBePlainText)
	}
	
	object JsonContentWriter
	{
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
	
	object XmlContentWriter
	{
		// COMPUTED ----------------------------
		
		/**
		 * @param naming Implicit element names to apply
		 * @return A new content writer that converts result values into XML elements
		 */
		def plain(implicit naming: XmlElementNames = XmlElementNames.default) = new PlainXmlContentWriter()
		/**
		 * @param naming Implicit element names to apply
		 * @return A content writer that always applies XML enveloping
		 */
		def enveloped(implicit naming: XmlEnvelopeNames = XmlEnvelopeNames.default) = new XmlEnveloper()
		
		
		// OTHER    ----------------------------
		
		/**
		 * Creates a content writer with optional enveloping support
		 * @param envelopHeaderNames Names of the headers that control enveloping. Default = "X-Envelop"
		 * @param envelopParamNames Names of the (query) parameters that control enveloping. Default = "envelop".
		 * @param envelopsByDefault Whether to envelope responses by default. Default = false.
		 * @param noEmptyEnvelopeElements Whether to exclude empty XML elements from the envelopes. Default = false.
		 * @param naming Implicit XML element names to use.
		 * @return A new content writer
		 */
		def apply(envelopHeaderNames: Iterable[String] = defaultEnvelopHeaderNames,
		          envelopParamNames: Iterable[String] = defaultEnvelopParamNames, envelopsByDefault: Boolean = false,
		          noEmptyEnvelopeElements: Boolean = false)
		         (implicit naming: XmlEnvelopeNames = XmlEnvelopeNames.default) =
			new XmlContentWriter(envelopHeaderNames, envelopParamNames, envelopsByDefault, noEmptyEnvelopeElements)
		
		
		// NESTED   ----------------------------
		
		object XmlElementNames
		{
			// ATTRIBUTES   -------------------
			
			/**
			 * The default XML element names to use
			 */
			lazy val default = apply()
			
			
			// OTHER    -----------------------
			
			/**
			 * Creates a set of XML element names
			 * @param root Name used for the root element. Default = "Response".
			 * @param description Name used for result descriptions. Default = "Message".
			 * @param listItem Name used for individual list/collection elements. Default = "ListItem".
			 * @param descriptionAttribute Name used for the description, when written as an XML attribute.
			 *                             Default = "description".
			 * @param listLengthAttribute Name used for the list length -attribute. Default = "length".
			 * @param namespace Implicit XML namespace to apply
			 * @return Specified XML element names
			 */
			def apply(root: String = "Response", description: String = "Message", listItem: String = "ListItem",
			          descriptionAttribute: String = "description", listLengthAttribute: String = "length")
			         (implicit namespace: Namespace = Namespace.empty): XmlElementNames =
				_XmlElementNames(namespace, root, description, listItem, descriptionAttribute, listLengthAttribute)
			
			private case class _XmlElementNames(namespace: Namespace, root: String, description: String,
			                                    listItem: String, descriptionAttribute: String,
			                                    listLengthAttribute: String)
				extends XmlElementNames
		}
		/**
		 * Specifies the element names used in XML responses
		 */
		trait XmlElementNames
		{
			/**
			 * @return XML namespace to apply thoughout the whole response
			 */
			def namespace: Namespace
			
			/**
			 * @return Name used for the root element.
			 */
			def root: String
			/**
			 * @return Name used for result descriptions
			 */
			def description: String
			/**
			 * @return Name used for individual list/collection elements.
			 */
			def listItem: String
			/**
			 * @return Name used for the description, when written as an XML attribute.
			 */
			def descriptionAttribute: String
			/**
			 * @return Name used for the list length -attribute
			 */
			def listLengthAttribute: String
		}
		
		object XmlEnveloper
		{
			// NESTED   ------------------------
			
			object XmlEnvelopeNames
			{
				/**
				 * Default XML element names
				 */
				lazy val default = apply()
			}
			/**
			 * Creates a set of XML element names
			 * @param root Name used for the root element. Default = "Response".
			 * @param value Name used for the element containing the result's value. Default = "Value".
			 * @param valueOnFailure Name used for the element containing the result's value in case of failure
			 *                       responses.
			 *                       Note that failure responses often specify a description instead of value.
			 *                       Default = "Value".
			 * @param description Name used for result descriptions. Default = "Message".
			 * @param status Name used for the response status element. Default = "Status".
			 * @param headers Name used for the XML element containing the response headers. Default = "Headers".
			 * @param listItem Name used for individual list/collection elements. Default = "ListItem".
			 * @param listLengthAttribute Name used for the list length -attribute. Default = "length".
			 * @param descriptionAttribute Name used for the description, when written as an XML attribute.
			 *                             Default = "description".
			 * @param namespace Implicit XML namespace to apply
			 */
			case class XmlEnvelopeNames(root: String = "Response", value: String = "value",
			                            valueOnFailure: String = "value", description: String = "Message",
			                            status: String = "Status", headers: String = "Headers",
			                            listItem: String = "ListItem", listLengthAttribute: String = "length",
			                            descriptionAttribute: String = "description")
			                           (implicit val namespace: Namespace = Namespace.empty)
				extends XmlElementNames
		}
		/**
		 * A content writer that always applies XML enveloping
		 * @param naming Implicit element names to apply
		 */
		class XmlEnveloper(noEmptyElements: Boolean = false)
		                  (implicit naming: XmlEnvelopeNames = XmlEnvelopeNames.default)
			extends ContentWriter[Any]
		{
			// ATTRIBUTES   -----------------------
			
			private implicit val namespace: Namespace = naming.namespace
			
			
			// COMPUTED ---------------------------
			
			/**
			 * @return A copy of this writer which includes empty XML elements
			 */
			def includingEmptyElements =
				if (noEmptyElements) new XmlEnveloper(noEmptyElements = false) else this
			/**
			 * @return A copy of this writer which excludes empty XML elements
			 */
			def withoutEmptyElements =
				if (noEmptyElements) this else new XmlEnveloper(noEmptyElements = true)
			
			
			// IMPLEMENTED  -----------------------
			
			override def prepare(content: ResponseContent, status: Status, headers: Headers)
			                    (implicit context: Any): (WriteResponseBody, Status) =
			{
				val baseElements = Vector(
					XmlElement(naming.status, status.code),
					toXml(if (status.isSuccess) naming.value else naming.valueOnFailure, content.value),
					XmlElement(naming.description, content.description),
					XmlElement(naming.headers,
						children = headers.fields.view
							.map { case (key, value) => XmlElement(key, value) }.toOptimizedSeq)
				)
				val body = WriteResponseBody.xml(XmlElement(naming.root,
					children = if (noEmptyElements) baseElements.filter { _.nonEmpty } else baseElements
				))
				// When enveloping, the outer status is always 200
				body -> OK
			}
		}
		
		/**
		 * A content writer that converts result values into XML elements
		 * @param naming Implicit element names to apply
		 */
		class PlainXmlContentWriter(implicit naming: XmlElementNames = XmlElementNames.default)
			extends ContentWriter[Any]
		{
			// COMPUTED ----------------------------
			
			private implicit def namespace: Namespace = naming.namespace
			
			
			// IMPLEMENTED  ------------------------
			
			override def prepare(content: ResponseContent, status: Status, headers: Headers)
			                    (implicit context: Any): (WriteResponseBody, Status) =
			{
				val body = {
					if (content.value.isEmpty) {
						// Case: No content => Empty body
						if (content.description.isEmpty)
							NoBody
						// Case: Only description
						else
							WriteResponseBody.xml(XmlElement(naming.root,
								children = Single(XmlElement(naming.description, content.description))))
					}
					// Case: Value included
					else {
						val xml = toXml(naming.root, content.value)
						// May also include an element for the description
						val described = {
							if (content.description.isEmpty)
								xml
							else
								xml.withAttribute(naming.descriptionAttribute, content.description)
						}
						WriteResponseBody.xml(described)
					}
				}
				body -> status
			}
		}
	}
	/**
	 * A content writer that always uses XML and supports optional enveloping
	 * @param envelopHeaderNames Names of the headers that control enveloping. Default = "X-Envelop"
	 * @param envelopParamNames Names of the (query) parameters that control enveloping. Default = "envelop".
	 * @param envelopsByDefault Whether to envelope responses by default. Default = false.
	 * @param noEmptyEnvelopeElements Whether to exclude empty XML elements from the envelopes. Default = false.
	 * @param naming Implicit XML element names to use.
	 * @return A new content writer
	 */
	class XmlContentWriter(override protected val envelopHeaderNames: Iterable[String] = defaultEnvelopHeaderNames,
	                       override protected val envelopParamNames: Iterable[String] = defaultEnvelopParamNames,
	                       override protected val envelopsByDefault: Boolean = false,
	                       noEmptyEnvelopeElements: Boolean = false)
	                      (implicit naming: XmlEnvelopeNames = XmlEnvelopeNames.default)
		extends PossiblyEnvelopingContentWriter[RequestContext[_]]
	{
		override protected lazy val envelopingDelegate: ContentWriter[RequestContext[_]] =
			new XmlEnveloper(noEmptyEnvelopeElements)
		override protected lazy val plainDelegate: ContentWriter[RequestContext[_]] = new PlainXmlContentWriter()
	}
}

/**
 * Common trait for interfaces that prepare [[ResponseContent]]s, so that they may be written into [[Response]] bodies
 * @tparam C Required contextual information
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.0
 */
trait ContentWriter[-C]
{
	// ABSTRACT ---------------------------
	
	/**
	 * Prepares response content, so that it may be written into the response body
	 * @param content Content to write the response body.
	 * @param status Status of the outgoing response
	 * @param headers Headers prepared for the outgoing response
	 * @param context Implicit contextual information
	 * @return Logic for writing the specified content into the response body,
	 *         plus the final status to yield
	 *         (some content writers place the status inside the content,
	 *         in which case the outer status may be different)
	 */
	def prepare(content: ResponseContent, status: Status, headers: Headers)
	           (implicit context: C): (WriteResponseBody, Status)
}
