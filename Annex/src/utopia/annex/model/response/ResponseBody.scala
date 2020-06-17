package utopia.annex.model.response

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.FromModelFactory

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
		/**
		  * @param parser Model parser
		  * @tparam A Type of parsed object
		  * @return Content of responses that return possibly multiple items
		  */
		override def vector[A](implicit parser: FromModelFactory[A]) = VectorContent(
			value.vectorOr(Vector(value)).flatMap { _.model })
		
		/**
		  * @param parser Model parser
		  * @tparam A Type of parsed object
		  * @return Content of responses that return only a single model
		  */
		def single[A](implicit parser: FromModelFactory[A]) = SingleContent(value.getModel)
	}
	
	/**
	  * Empty response content
	  */
	case object Empty extends ResponseBody(Value.empty)
	{
		override def vector[A](implicit parser: FromModelFactory[A]) = VectorContent(Vector())
	}
}