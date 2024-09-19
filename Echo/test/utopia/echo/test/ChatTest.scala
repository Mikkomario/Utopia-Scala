package utopia.echo.test

import utopia.access.http.Status
import utopia.annex.util.RequestResultExtensions._
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.controller.AccessLogger
import utopia.echo.controller.{Chat, OllamaClient}
import utopia.echo.model.enumeration.ModelParameter
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.Wait
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.test.TestContext._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.logging.FileLogger

import scala.io.StdIn

/**
  * A console-based chat interface
  * @author Mikko Hilpinen
  * @since 17.09.2024, v1.1
  */
object ChatTest extends App
{
	// Sets up the context
	Status.setup()
	
	private implicit val jsonParser: JsonParser = JsonBunny
	private val accessLogger = new AccessLogger(new FileLogger("Echo/data/test-output", groupDuration = 1.seconds))
	
	private val client = new OllamaClient(
		requestInterceptors = Single(accessLogger),
		responseInterceptors = Single(accessLogger))
		
	// Prompts the user to select the LLM to use
	client.localModels.future.waitForResult().toTry.logToOption.foreach { llms =>
		println("Please select the LLM to use")
		StdIn.selectFrom(llms.map { llm => llm.designator -> s"${ llm.name } (${ llm.sizeBytes / 1000000000L } Gb)" })
			.foreach { implicit llm =>
				val chat = new Chat(client)
				
				// Sets up data-listening
				chat.lastContextSizePointer.addContinuousListener { e =>
					println(s"Used context size: ${ e.newValue }")
				}
				chat.largestContextIncreasePointer.addContinuousListener { e =>
					println(s"Largest context size increase: ${ e.newValue }")
				}
				chat.messageHistoryPointer.addContinuousListener { e =>
					println(s"Message history: ${ e.newValue.size } messages")
				}
				
				// Sets up the system message
				println("Please specify the system message to use. Empty line ends input. If left empty, model file message will be applied instead.")
				OptionsIterator.continually(StdIn.readNonEmptyLine()).mkString("\n").trim
					.ifNotEmpty.foreach(chat.appendSystemMessage)
				
				// Starts the interaction loop
				println("Welcome. Write a message to ask something. Write /clear to clear the conversation history. Write /bye to exit. Write /options to modify model parameters.")
				println("Note: Currently only single line messages are supported.")
				var open = true
				while (open) {
					println("Waiting for the next input")
					val input = StdIn.readLine().trim
					if (input.startsWith("/bye"))
						open = false
					else if (input.startsWith("/clear")) {
						chat.clearMessageHistory()
						println("Message history cleared")
					}
					else if (input.startsWith("/options")) {
						val currentOptions = chat.options
						StdIn.selectFrom(ModelParameter.values.map { p => p -> s"${ p.key }${
							currentOptions.getOrElse(p, Value.empty).getString
								.mapIfNotEmpty { v => s" (currently $v)" } }" }, maxListCount = 50)
							.foreach { param =>
								println(s"Please specify a new value for ${ param.key }.")
								val current = currentOptions.getOrElse(param, Value.empty)
								if (current.isEmpty)
									println("Empty cancels")
								else
									println("Empty clears the current value")
								
								StdIn.readNonEmptyLine() match {
									case Some(newValue) =>
										chat(param) = newValue
										println(s"${ param.key } assigned to $newValue")
									case None =>
										if (current.nonEmpty) {
											chat.clear(param)
											println(s"${ param.key } cleared")
										}
								}
							}
					}
					else if (input.nonEmpty) {
						val schrodinger = chat.push(input)
						println("\nWaiting for the response...")
						schrodinger.manifest.newTextPointer.addContinuousListener { e => print(e.newValue) }
						
						schrodinger.finalResultFuture.waitFor().flatMap { _.wrapped }.logToOption.foreach { reply =>
							Wait(0.2.seconds)
							println(s"\n\n${ reply.statistics }\n")
						}
					}
				}
			}
	}
	
	println("Bye!")
}
