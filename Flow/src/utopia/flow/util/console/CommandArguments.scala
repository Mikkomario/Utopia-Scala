package utopia.flow.util.console

import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.JsonParser
import utopia.flow.util.StringExtensions._

import scala.collection.immutable.VectorBuilder

object CommandArguments
{
	/**
	 * @param schema Command schemas
	 * @param input Input arguments
	 * @param jsonParser json parser used (implicit)
	 * @return New command arguments set
	 */
	def apply(schema: Vector[ArgumentSchema], input: Vector[String])
	         (implicit jsonParser: JsonParser): CommandArguments =
		apply(CommandArgumentsSchema(schema), input)
}

/**
 * A class used for handling console command arguments (like "1.2.3 -p" etc.)
 * @author Mikko Hilpinen
 * @since 26.6.2021, v1.10
 */
case class CommandArguments(schema: CommandArgumentsSchema, input: Vector[String])(implicit jsonParser: JsonParser)
	extends ModelConvertible
{
	// ATTRIBUTES   -------------------------
	
	val (values, unrecognized) =
	{
		// Groups the input into categories:
		// Boolean Flags (-p, -more etc.)
		val flagsBuilder = new VectorBuilder[ArgumentSchema]()
		// Named values (name="James" count = 3 etc.)
		val namedBuilder = new VectorBuilder[(ArgumentSchema, Value)]()
		// Unnamed values as an ordered list
		val listBuilder = new VectorBuilder[String]()
		// Unrecognized input (not matching any schema)
		val unrecognizedBuilder = new VectorBuilder[String]()
		
		input.foreach { argument =>
			if (argument.contains("="))
			{
				val (rawKey, rawValue) = argument.splitAtFirst("=")
				schema(rawKey.trim) match
				{
					case Some(schema) => namedBuilder += schema -> jsonParser.valueOf(rawValue.trim)
					case None => unrecognizedBuilder += argument
				}
			}
			else if (argument.startsWith("-"))
				schema(argument.drop(1).trim) match
				{
					case Some(schema) => flagsBuilder += schema
					case None => unrecognizedBuilder += argument
				}
			else
				listBuilder += argument.trim
		}
		
		// Assigns the unnamed parameters to schemas
		val flags = flagsBuilder.result()
		val named = namedBuilder.result()
		val specifiedSchemas = flags.toSet ++ named.map { _._1 }
		val openSchemas = schema.arguments.filterNot(specifiedSchemas.contains)
		val unnamed = listBuilder.result()
		val parsedList = unnamed.zip(openSchemas).map { case (argument, schema) => schema -> jsonParser.valueOf(argument) }
		
		val definedValues = (named ++ parsedList ++ flags.map { _ -> (true: Value) }).toMap
		val finalValues = schema.arguments.map { schema =>
			schema -> definedValues.getOrElse(schema, schema.defaultValue) }.toMap
		
		(finalValues, unrecognizedBuilder.result() ++ unnamed.drop(openSchemas.size))
	}
	
	
	// IMPLEMENTED  --------------------------
	
	override def toString = toJson
	
	override def toModel = Model.withConstants(
		values.toVector.map { case (schema, value) => Constant(schema.name, value) })
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param schema Schema of the argument to read
	 * @return Argument value
	 */
	def apply(schema: ArgumentSchema) = values.getOrElse(schema, schema.defaultValue)
	
	/**
	 * @param parameterName Argument name to read
	 * @return Argument value
	 */
	def apply(parameterName: String): Value = schema(parameterName) match
	{
		case Some(schema) => apply(schema)
		case None => Value.empty
	}
}
