package utopia.bunnymunch.jawn

import java.io.{File, InputStream}
import java.nio.file.Path

import org.typelevel.jawn.Parser
import utopia.flow.util.StringFrom

import scala.io.Codec

/**
  * Used for parsing json (uses jawn internally)<br>
  * See: https://github.com/typelevel/jawn
  * @author Mikko Hilpinen
  * @since 12.5.2020, v
  */
object JsonBunny
{
	// ATTRIBUTES	----------------------------
	
	private implicit val defaultEncoding: Codec = Codec.UTF8
	private implicit val facade: ValueFacade.type = ValueFacade
	
	
	// OTHER	--------------------------------
	
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
