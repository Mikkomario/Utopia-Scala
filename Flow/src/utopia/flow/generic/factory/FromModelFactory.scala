package utopia.flow.generic.factory

import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.parse.json.{JsonReader, JsonParser}

import scala.language.implicitConversions
import scala.util.Try

object FromModelFactory
{
	// OTHER    ---------------------
	
	/**
	 * @param f A parsing function that accepts a model and returns parsed item or a failure
	 * @tparam A Type of parsed items
	 * @return A new from model factory
	 */
	implicit def apply[A](f: ModelLike[Property] => Try[A]): FromModelFactory[A] = new FunctionalFactory[A](f)
	
	
	// NESTED   ---------------------
	
	private class FunctionalFactory[+A](f: ModelLike[Property] => Try[A]) extends FromModelFactory[A]
	{
		override def apply(model: ModelLike[Property]): Try[A] = f(model)
	}
}

/**
  * This trait is extended by instance factories that can convert model data into object data.
  * The factory may make assumptions about the type of model data and may give more sensible results
  * with other models than with others.
  */
trait FromModelFactory[+A]
{
	// ABSTRACT METHODS    ----------------------
	
	/**
	  * Parses an instance by reading the data from a model instance
	  * @param model Model data is parsed from
	  * @return an instance parsed from model data. Failure if no instance could be parsed.
	  */
	def apply(model: ModelLike[Property]): Try[A]
	
	
	// OTHER METHODS   --------------------------
	
	/**
	  * Parses an instance from a JSON string. Returns none if either the JSON string couldn't be
	  * parsed or if the instance couldn't be parsed from read data.
	  */
	def fromJson(json: String)(implicit parser: JsonParser) = parser(json).map(v => apply(v.getModel)).flatten
	
	@deprecated("Please use fromJson instead", "< v2.3")
	def fromJSON(json: String) = JsonReader(json).map(v => apply(v.getModel)).flatten
	
	/**
	  * @param f A mapping function for parse results
	  * @tparam B Type of mapped parse results
	  * @return A factory that utilizes this factory, but also applies the specified mapping function
	  */
	def mapParseResult[B](f: A => B) = FromModelFactory { apply(_).map(f) }
	/**
	  * @param f A mapping function for parse results. May return a failure.
	  * @tparam B Type of mapped parse results, when successful
	  * @return A factory that utilizes this factory, but also applies the specified mapping function
	  */
	def flatMapParseResult[B](f: A => Try[B]) = FromModelFactory { apply(_).flatMap(f) }
}
