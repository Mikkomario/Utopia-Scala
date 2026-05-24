package utopia.flow.generic.factory

import utopia.flow.generic.factory.FromModelFactory.{MappedFactory, PreprocessingFactory, TryMappedFactory}
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.util.Mutate

import java.nio.file.Path
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
	implicit def apply[A](f: HasProperties => Try[A]): FromModelFactory[A] = new _FromModelFactory[A](f)
	
	
	// NESTED   ---------------------
	
	private class PreprocessingFactory[A](delegate: FromModelFactory[A], preprocess: Mutate[HasProperties])
		extends FromModelFactory[A]
	{
		override def apply(model: HasProperties): Try[A] = delegate(preprocess(model))
	}
	
	private class TryMappedFactory[O, R](delegate: FromModelFactory[O], f: O => Try[R]) extends FromModelFactory[R]
	{
		override def apply(model: HasProperties): Try[R] = delegate(model).flatMap(f)
	}
	private class MappedFactory[O, R](delegate: FromModelFactory[O], f: O => R) extends FromModelFactory[R]
	{
		override def apply(model: HasProperties): Try[R] = delegate(model).map(f)
	}
	
	private class _FromModelFactory[+A](f: HasProperties => Try[A]) extends FromModelFactory[A]
	{
		override def apply(model: HasProperties): Try[A] = f(model)
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
	def apply(model: HasProperties): Try[A]
	
	
	// OTHER METHODS   --------------------------
	
	/**
	  * Parses an instance from a JSON string. Returns none if either the JSON string couldn't be
	  * parsed or if the instance couldn't be parsed from read data.
	  */
	def fromJson(json: String)(implicit parser: JsonParser) = parser(json).map(v => apply(v.getModel)).flatten
	@deprecated("Please use fromJson instead", "< v2.3")
	def fromJSON(json: String) = JsonReader(json).map(v => apply(v.getModel)).flatten
	
	/**
	 * Parses the contents of a JSON file.
	 * Assumes that the file contains a single JSON object.
	 * @param jsonFilePath Path to the JSON file to parse
	 * @param jsonParser Implicit JSON parser used
	 * @return Parsed instance. Failure if parsing or file-reading failed.
	 */
	def fromJsonFile(jsonFilePath: Path)(implicit jsonParser: JsonParser) =
		jsonParser(jsonFilePath).flatMap { _.tryModel.flatMap(apply) }
	@deprecated("Renamed to .fromJsonFile(Path)", "v2.9")
	def fromPath(jsonFilePath: Path)(implicit jsonParser: JsonParser) = fromJsonFile(jsonFilePath)
	
	/**
	 * @param f A mapping function applied to this factory's parse results
	 * @tparam B Type of mapping results
	 * @return A factory that yields mapped items
	 */
	def mapResult[B](f: A => B): FromModelFactory[B] = new MappedFactory[A, B](this, f)
	/**
	 * @param f A mapping function applied to this factory's parse results. May yield a failure.
	 * @tparam B Type of mapping results
	 * @return A factory that yields mapped items
	 */
	def tryMapResult[B](f: A => Try[B]): FromModelFactory[B] = new TryMappedFactory[A, B](this, f)
	
	@deprecated("Renamed to .mapResult(...)", "v2.9")
	def mapParseResult[B](f: A => B) = mapResult(f)
	@deprecated("Renamed to .tryMapResult(...)", "v2.9")
	def flatMapParseResult[B](f: A => Try[B]) = tryMapResult(f)
	
	/**
	 * @param f A function that prepares / mutates models that are to be parsed by this factory
	 * @return A copy of this factory, which applies the specified prepare function
	 */
	def preparingWith(f: Mutate[HasProperties]): FromModelFactory[A] = new PreprocessingFactory[A](this, f)
}
