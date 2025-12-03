package utopia.nexus.controller.write

import utopia.access.model.Headers
import utopia.access.model.enumeration.Status
import utopia.access.model.enumeration.Status.OK
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.mutable.DataType.{ModelType, PairType, StringType, VectorType}
import utopia.flow.parse.xml.{Namespace, NamespacedString, XmlElement}
import utopia.nexus.controller.write.WriteResponseBody.NoBody
import utopia.nexus.controller.write.XmlContentWriter.{PlainXmlContentWriter, XmlEnveloper}
import utopia.nexus.controller.write.XmlContentWriter.XmlEnveloper.XmlEnvelopeNames
import utopia.nexus.model.request.RequestContext
import utopia.nexus.model.response.ResponseContent

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
		case class XmlEnvelopeNames(root: String = "Response", value: String = "Value",
		                            valueOnFailure: String = "Value", description: String = "Message",
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
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.0
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
