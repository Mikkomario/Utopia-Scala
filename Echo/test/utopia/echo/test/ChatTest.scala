package utopia.echo.test

import utopia.annex.util.RequestResultExtensions._
import utopia.echo.controller.{Chat, EstimateTokenCount}
import utopia.echo.model.LlmDesignator
import utopia.echo.model.enumeration.ModelParameter
import utopia.echo.model.request.llm.CreateModelRequest
import utopia.echo.model.response.llm.StreamedStatus
import utopia.echo.test.EchoTestContext._
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.Wait
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.console.ConsoleExtensions._

import scala.io.StdIn
import scala.util.{Failure, Success}

/**
  * A console-based chat interface
  * @author Mikko Hilpinen
  * @since 17.09.2024, v1.1
  */
object ChatTest extends App
{
	// APP CODE -----------------------
	
	// Prompts the user to select the LLM to use
	selectModel().foreach { originalLlm =>
		// Sets up the system message
		println("Looking up the model's system message...")
		val currentSystemMessage = client.showModel(originalLlm).future.waitForResult().toTry match {
			case Success(info) => info.systemMessage
			case Failure(error) =>
				log(error, "Failed to acquire model info")
				""
		}
		val (newLlm, originalSystemMessage, newSystemMessage) = {
			if (currentSystemMessage.isEmpty) {
				println("Currently there is no system message defined")
				val newSystemMessage = {
					if (StdIn.ask("Do you want to specify a system message for the duration of this session?",
						default = true))
						requestSystemMessage()
					else
						None
				}
				(originalLlm, "", newSystemMessage)
			}
			else {
				println(s"The current system message is: $currentSystemMessage")
				if (StdIn.ask("\nDo you want to keep this system message in effect?", default = true))
					(originalLlm, currentSystemMessage, None)
				else
					StdIn.readNonEmptyLine(
							"Please specify a name for the new model version, which won't include a system message")
						.flatMap { newModelName =>
							println("Creating a new model...")
							client.push(
									CreateModelRequest.streamed.apply(newModelName, s"FROM $originalLlm\nSYSTEM \n"))
								.future.waitForResult().toTry.logToOption
								.flatMap { statusStream =>
									// Prints the model-creation status
									statusStream.statusPointer.addContinuousListenerAndSimulateEvent("") { e =>
										println(e.newValue)
									}
									statusStream.finalStatusFuture.waitForResult().logToOption
								}
								.flatMap { finalStatus =>
									if (finalStatus ~== StreamedStatus.expectedFinalStatus)
										Some(LlmDesignator(newModelName))
									else {
										println(s"Expected \"${
											StreamedStatus.expectedFinalStatus }\", received \"$finalStatus\"")
										None
									}
								}
						}
						.map { newLlm =>
							val newSystemMessage = {
								if (StdIn.ask(
									"\nDo you want to specify a new system message instead (only for this session)?",
									default = true))
									requestSystemMessage()
								else
									None
							}
							(newLlm, "", newSystemMessage)
						}
						.getOrElse {
							println(s"Defaults back to $originalLlm")
							(originalLlm, currentSystemMessage, None)
						}
			}
		}
		
		newLlm.use { implicit llm =>
			val chat = new Chat(client)
			
			// Sets up data-listening
			chat.lastContextSizePointer.addContinuousListener { e =>
				println(s"Used context size: ${ e.newValue } tokens")
			}
			chat.largestReplySizePointer.addContinuousListener { e =>
				println(s"Largest reply so far: ${ e.newValue } tokens")
			}
			chat.messageHistoryPointer.addContinuousListener { e =>
				println(s"Message history: ${ e.newValue.size } messages")
			}
			
			chat.defaultSystemMessageTokensPointer.value = EstimateTokenCount.in(originalSystemMessage)
			newSystemMessage.foreach(chat.appendSystemMessage)
			
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
	
	
	// OTHER    ----------------------------
	
	private def requestSystemMessage() = {
		println("Please specify the system message to use. Empty line ends input.")
		OptionsIterator.continually(StdIn.readNonEmptyLine()).mkString("\n").trim
			.ifNotEmpty
	}
}
