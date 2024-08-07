package utopia.annex.model.response

import utopia.annex.model.error.EmptyResponseException
import utopia.annex.model.response.ResponseBody.Content
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.MaybeEmpty

import scala.util.{Failure, Try}

/**
  * Represents a body of a successful response
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
@deprecated("Deprecated for removal. The new RequestResult structure doesn't utilize this model anymore.", "v1.8")
sealed abstract class ResponseBody(private val body: Value) extends MaybeEmpty[ResponseBody]
{
	// ABSTRACT	--------------------------
	
	/**
	  * @param parser Parser used for parsing vector contents
	  * @tparam A Type of parsed content
	  * @return Response content as a vector
	  */
	def vector[A](implicit parser: FromModelFactory[A]): VectorContent[A]
	
	/**
	  * Attempts to parse response body contents, if available
	  * @param parser Parser used for body content, if one is present
	  * @tparam A Type of parsed instance
	  * @return Parsed instance. Failure if this body is empty or if parsing failed.
	  */
	def tryParseSingleWith[A](parser: FromModelFactory[A]): Try[A]
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return Value of this response
	  */
	def value = body
	
	/**
	  * Attempts to parse response body contents, if available
	  * @param parser Parser used for body content, if one is present (implicit)
	  * @tparam A Type of parsed instance
	  * @return Parsed instance. Failure if this body is empty or if parsing failed.
	  */
	def parsedSingle[A](implicit parser: FromModelFactory[A]) = tryParseSingleWith(parser)
	/**
	  * Attempts to parse the contents of this response body into 0-n items
	  * @param parser Parser used for handling json models
	  * @tparam A Type of parsed items
	  * @return Parsed items. Failure if parsing failed for any item.
	  */
	def parseMany[A](implicit parser: FromModelFactory[A]) = vector.parsed
	
	
	// IMPLEMENTED  ----------------------
	
	override def self = this
	
	/**
	  * @return Whether this response is empty
	  */
	override def isEmpty = body.isEmpty
	
	override def notEmpty = this match {
		case c: Content => Some(c)
		case _ => None
	}
}

@deprecated("Deprecated for removal. The new RequestResult structure doesn't utilize this model anymore.", "v1.8")
object ResponseBody
{
	// OTHER	--------------------------
	
	/**
	  * @param value Response body value (may be empty)
	  * @return Either Content (if value is non-empty) or Empty (if value is empty)
	  */
	def apply(value: Value): ResponseBody = if (value.isDefined) Content(value) else Empty
	
	
	// NESTED	--------------------------
	
	/**
	  * Non-empty response content
	  * @param body Response value
	  */
	@deprecated("Deprecated for removal. The new RequestResult structure doesn't utilize this model anymore.", "v1.8")
	case class Content private(body: Value) extends ResponseBody(body)
	{
		// COMPUTED -----------------------------
		
		/**
		  * @param parser Model parser
		  * @tparam A Type of parsed object
		  * @return Content of responses that return only a single model
		  */
		def single[A](implicit parser: FromModelFactory[A]) = SingleContent(value.getModel)
		
		
		// IMPLEMENTED  --------------------------
		
		/**
		  * @param parser Model parser
		  * @tparam A Type of parsed object
		  * @return Content of responses that return possibly multiple items
		  */
		override def vector[A](implicit parser: FromModelFactory[A]) = VectorContent(
			value.vectorOr(Vector(value)).flatMap { _.model })
		
		override def tryParseSingleWith[A](parser: FromModelFactory[A]) = single(parser).parsed
	}
	
	/**
	  * Empty response content
	  */
	@deprecated("Deprecated for removal. The new RequestResult structure doesn't utilize this model anymore.", "v1.8")
	case object Empty extends ResponseBody(Value.empty)
	{
		override def vector[A](implicit parser: FromModelFactory[A]) =
			VectorContent(utopia.flow.collection.immutable.Empty)
		
		override def tryParseSingleWith[A](parser: FromModelFactory[A]) =
			Failure(new EmptyResponseException("Can't parse item from an empty response body"))
	}
}