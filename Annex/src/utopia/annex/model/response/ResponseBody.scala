package utopia.annex.model.response

import utopia.annex.model.error.EmptyResponseException
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.FromModelFactory

import scala.util.{Failure, Try}

/**
  * Represents a body of a successful response
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
sealed abstract class ResponseBody(private val body: Value)
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
	  * @return Whether this response contains data
	  */
	def nonEmpty = body.isDefined
	
	/**
	  * @return Whether this response is empty
	  */
	def isEmpty = body.isEmpty
	
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
}

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
	case object Empty extends ResponseBody(Value.empty)
	{
		override def vector[A](implicit parser: FromModelFactory[A]) = VectorContent(Vector())
		
		override def tryParseSingleWith[A](parser: FromModelFactory[A]) =
			Failure(new EmptyResponseException("Can't parse item from an empty response body"))
	}
}