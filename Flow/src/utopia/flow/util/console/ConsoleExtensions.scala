package utopia.flow.util.console

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.JsonParser
import utopia.flow.util.CollectionExtensions._
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
		 * @param retryPrompt Prompt used for asking the user for another input in case they specify an empty input -
		 *                    Will only be asked once (default = empty = no retry prompting)
		 * @return Read line. None if user wrote an empty line.
		 */
		def readNonEmptyLine(prompt: String = "", retryPrompt: String = ""): Option[String] =
		{
			val firstResult = printAndReadLine(prompt).notEmpty
			if (firstResult.isDefined || retryPrompt.isEmpty)
				firstResult
			else
				readNonEmptyLine(retryPrompt)
		}
		
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
			readNonEmptyLine(question + s" (y/n - default=${if (default) "yes" else "no"})") match
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
		/**
		 * Reads lines until a non-empty value is given
		 */
		def readUntilNotEmpty(prompt: String = "") =
		{
			prompt.notEmpty.foreach(println)
			readIterator.find { _.isDefined }.get
		}
		
		/**
		 * Reads either a valid input or an empty input
		 * @param initialPrompt Prompt shown before initial read (default = empty = no prompt)
		 * @param validate A function that validates the input. Returns Left[String] error message /
		 *                 instructions on failure and Right[A] valid input on success.
		 * @tparam A Type of valid item
		 * @return Read valid item or None if user provided an empty input
		 */
		def readValidOrEmpty[A](initialPrompt: String = "")(validate: Value => Either[String, A]) =
		{
			initialPrompt.notEmpty.foreach(println)
			readIterator.findMap { input =>
				if (input.isEmpty)
					Some(None)
				else
					validate(input) match
					{
						case Right(valid) => Some(Some(valid))
						case Left(message) =>
							println(message)
							None
					}
			}.get
		}
	}
}
