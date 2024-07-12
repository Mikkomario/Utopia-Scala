package utopia.bunnymunch.jawn

import org.typelevel.jawn.Parser
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.StringFrom

import java.io.{File, InputStream}
import java.nio.file.Path
import scala.io.Codec

/**
  * Used for parsing json (uses jawn internally)<br>
  * See: https://github.com/typelevel/jawn
  * @author Mikko Hilpinen
  * @since 12.5.2020, v
  */
// TODO: Add support for asynchronous parsing using Parser.async
object JsonBunny extends JsonParser
{
	// ATTRIBUTES	----------------------------
	
	/**
	  * The encoding accepted by this parser (currently only supports utf-8)
	  */
	implicit val defaultEncoding: Codec = Codec.UTF8
	private implicit val facade: ValueFacade.type = ValueFacade
	
	
	// IMPLEMENTED	----------------------------
	
	override def apply(json: String) = munch(json)
	
	override def apply(file: File) = munchFile(file)
	
	override def apply(inputStream: InputStream) = munchStream(inputStream)
	
	
	// OTHER	--------------------------------
	
	/**
	  * @param s A string possibly representing a json value
	  * @return Json value of the string, if it could be parsed. If not, simply returns the value as a string.
	  */
	def sureMunch(s: String) = munch(s).toOption.getOrElse(s: Value)
	
	/**
	  * @param json Json to parse
	  * @return Parsed value from json. Failure if parsing failed.
	  */
	def munch(json: String) = Parser.parseFromString(json)
	
	/**
	  * @param inputStream Json stream to parse (will be buffered to string)
	  * @return Parsed json value. Failure if stream read or parse failed.
	  */
	def munchStream(inputStream: InputStream) = StringFrom.stream(inputStream).flatMap(munch)
	
	/**
	  * @param file A file
	  * @return Json value read from that file
	  */
	def munchFile(file: File) = Parser.parseFromFile(file)
	
	/**
	  * @param path File path
	  * @return Json value read from that path
	  */
	def munchPath(path: Path) = munchFile(path.toFile)
}
