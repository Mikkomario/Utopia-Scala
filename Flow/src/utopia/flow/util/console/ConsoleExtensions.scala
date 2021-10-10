package utopia.flow.util.console

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.JsonParser
import utopia.flow.util.StringExtensions._

import scala.io.StdIn

/**
 * Introduces extensions targeted for command line / console use cases
 * @author Mikko Hilpinen
 * @since 10.10.2021, v1.13
 */
object ConsoleExtensions
{
	implicit class RichStdIn(val in: StdIn.type) extends AnyVal
	{
		/**
		 * Reads a line of text from this input. May display a prompt before requesting input.
		 * @param prompt Prompt to display before requesting for input. If empty, no prompt is displayed (default)
		 * @return Read line as a string
		 */
		def printAndReadLine(prompt: String) =
		{
			prompt.notEmpty.foreach(println)
			in.readLine()
		}
		/**
		 * Reads a line of text from this input
		 * @param prompt Prompt to display before requesting input (default = empty = no prompt)
		 * @return Read line. None if user wrote an empty line.
		 */
		def readNonEmptyLine(prompt: String = "") = printAndReadLine(prompt).notEmpty
		
		/**
		 * Reads a value from this input
		 * @param prompt Prompt to display before requesting input (default = empty = no prompt)
		 * @return Value read from the input. Empty if user specified an empty string.
		 */
		def read(prompt: String = ""): Value =
			readNonEmptyLine(prompt) match
			{
				case Some(line) => line
				case None => Value.empty
			}
		/**
		 * Reads a value from this input, supports json parsing
		 * @param prompt Prompt to display before requesting input (default = empty = no prompt)
		 * @return Value read from the input. Empty if user specified an empty string.
		 */
		def readJson(prompt: String = "")(implicit parser: JsonParser): Value =
			readNonEmptyLine(prompt) match
			{
				case Some(line) => parser.valueOf(line)
				case None => Value.empty
			}
		
		/**
		 * Asks the user a yes or no -question
		 * @param question Question to ask from the user (" (y/n)" is added after the question)
		 * @param default The default value to use when answer can't be interpreted (default = false)
		 * @return Answer given by the user (or the default)
		 */
		def ask(question: String, default: => Boolean = false) =
			readNonEmptyLine(question + " (y/n)") match
			{
				case Some(answer) =>
					val firstChar = answer.head.toLower
					if (firstChar == 'y') true else if (firstChar == 'n') false else default
				case None => default
			}
		
		/**
		 * @return An iterator that reads new lines
		 */
		def readLineIterator = Iterator.continually { in.readLine() }
		/**
		 * @return An iterator that reads values
		 */
		def readIterator = Iterator.continually { read() }
		
		/**
		 * @return Reads lines until a non-empty response is given
		 */
		def readLineUntilNotEmpty(prompt: String = "") =
		{
			prompt.notEmpty.foreach(println)
			readLineIterator.find { _.nonEmpty }.get
		}
		/**
		 * @return Reads lines while a non-empty response is given
		 */
		def readLineWhileNotEmpty(prompt: String = "") =
		{
			prompt.notEmpty.foreach(println)
			readLineIterator.takeWhile { _.nonEmpty }.toVector
		}
	}
}
