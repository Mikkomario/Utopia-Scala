package utopia.echo.test

import utopia.annex.util.RequestResultExtensions._
import utopia.echo.controller.{Chat, EstimateTokenCount}
import utopia.echo.model.enumeration.ModelParameter
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.test.EchoTestContext._
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.Wait
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.view.mutable.Pointer

import java.nio.file.Paths
import scala.io.StdIn
import scala.util.{Failure, Success}

/**
  * A console-based chat interface
  * @author Mikko Hilpinen
  * @since 17.09.2024, v1.1
  */
object ChatTest extends App
{
	// ATTRIBUTES   -------------------
	
	private val multiLineIndicator = "\"\"\""
	
	
	// APP CODE -----------------------
	
	// Prompts the user to select the LLM to use
	selectModel().foreach { initialLlm =>
		val llmPointer = Pointer.eventful(initialLlm)
		implicit def llm: LlmDesignator = llmPointer.value
		def llm_=(newLlm: LlmDesignator) = llmPointer.value = newLlm
		
		// Sets up the system message
		println("Looking up the model's system message...")
		val currentSystemMessage = client.showModel.future.waitForResult().toTry match {
			case Success(info) => info.systemMessage
			case Failure(error) =>
				log(error, "Failed to acquire model info")
				""
		}
		val (originalSystemMessage, newSystemMessage) = {
			if (currentSystemMessage.isEmpty) {
				println("Currently there is no system message defined")
				val newSystemMessage = {
					if (StdIn.ask("Do you want to specify a system message for the duration of this session?",
						default = true))
						requestSystemMessage()
					else
						None
				}
				"" -> newSystemMessage
			}
			else {
				println(s"The current system message is: $currentSystemMessage")
				if (StdIn.ask("\nDo you want to overwrite this message?"))
					currentSystemMessage -> requestSystemMessage()
				else
					currentSystemMessage -> None
			}
		}
		
		val chat = new Chat(client, llm)
		chat.setupAutoSummaries()
		llmPointer.addContinuousListener { e => chat.llm = e.newValue }
		
		// Sets up data-listening
		chat.usedContextSizePointer.addContinuousListener { e =>
			println(s"\nUsed context size: ${ e.newValue } tokens")
		}
		chat.largestReplySizePointer.addContinuousListener { e =>
			println(s"\nLargest reply so far: ${ e.newValue } tokens")
		}
		chat.messageHistoryPointer.addContinuousListener { e =>
			println(s"\nMessage history: ${ e.newValue.size } messages")
		}
		chat.summarizingFlag.addContinuousListener { e =>
			if (e.newValue)
				println("Summarizing message history...")
			else {
				println("Message history summarized:")
				chat.messageHistory.lastOption.foreach { message => println(message.text) }
				println()
			}
		}
		
		chat.defaultSystemMessageTokensPointer.value = EstimateTokenCount.in(originalSystemMessage)
		newSystemMessage.foreach(chat.appendSystemMessage)
		
		// Starts the interaction loop
		println("Welcome. Write a message to ask something.")
		println("You also have the following commands available to you:")
		println("\t/bye or /exit - Closes this application")
		println("\t/clear - Clears the conversation history")
		println("\t/undo - Clears the latest query and its reply")
		println("\t/options - Modifies LLM parameters")
		println("\t/settings - Modifies chat settings")
		println("\t/summarize - Summarizes the conversation history in order to conserve context space")
		println("\t/save - Saves the current chat to a local file")
		println(s"You can write multi-line messages if you start and end the message with $multiLineIndicator.")
		var open = true
		while (open) {
			println("Waiting for the next input")
			val input = requestMultiLineString() match {
				case Some(input) => input.trim
				case None => ""
			}
			if (input.startsWith("/bye") || input.startsWith("/exit"))
				open = false
			else if (input.startsWith("/clear")) {
				chat.clearMessageHistory()
				println("Message history cleared")
			}
			else if (input.startsWith("/undo")) {
				val changed = chat.messageHistoryPointer.mutate { history => history.nonEmpty -> history.dropRight(2) }
				if (changed)
					println("The latest query & reply were removed from the chat history")
				else
					println("There were no messages to remove")
			}
			else if (input.startsWith("/options")) {
				var shouldContinue = true
				while (shouldContinue) {
					println("\nSelect option to target. To quit modifying options, just press enter.")
					val currentOptions = chat.options
					StdIn.selectFrom(ModelParameter.values.map { p => p -> s"${ p.key }${
							currentOptions(p).getString.mapIfNotEmpty { v => s" (currently $v)" } }" }, maxListCount = 50) match
					{
						case Some(param) =>
							println(s"Please specify a new value for ${ param.key }.")
							val current = currentOptions.get(param)
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
						case None => shouldContinue = false
					}
				}
			}
			else if (input.startsWith("/settings")) {
				println("Which setting do you want to modify?")
				StdIn.selectFrom(Vector(
					1 -> "LLM", 2 -> "Maximum context size", 3 -> "Minimum context size",
					4 -> "Expected reply size", 5 -> "Additional context size",
					6 -> "System message", 7 -> "Auto summary thresholds"))
					.foreach {
						case 1 => selectModel().foreach(llm_=)
						case 2 =>
							StdIn.read("Please specify the new maximum context size in tokens").int.foreach { size =>
								chat.maxContextSize = size
								println(s"Maximum context size set to $size")
							}
						case 3 =>
							StdIn.read("Please specify the new minimum context size in tokens").int.foreach { size =>
								chat.minContextSize = size
								println(s"Set minimum context size to $size")
							}
						case 4 =>
							StdIn.read("Please specify the expected reply size in tokens").int.foreach { size =>
								chat.expectedReplySize = size
								println(s"Expected reply size is now $size")
							}
						case 5 =>
							StdIn.read("Please specify the additional context size to apply").int.foreach { size =>
								chat.additionalContextSize = size
								println(s"Additional context size set to $size")
							}
						case 6 =>
							requestSystemMessage().foreach { systemMessage =>
								println("How do you want to apply this message?")
								StdIn.selectFrom(Vector(
										1 -> "Overwrite the current system message",
										2 -> "Append this to the current system message",
										3 -> "Cancel"))
									.foreach {
										case 1 =>
											chat.systemMessages = Single(systemMessage)
											println("System message overwritten")
										case 2 =>
											chat.systemMessages :+= systemMessage
											println("System message added")
										case _ => ()
									}
							}
						case 7 =>
							StdIn.read("At which context size should auto-summarization trigger?").int.foreach { size =>
								StdIn.read("How many messages should there least be in the chat history (counting both queries & replies separately)?")
									.int
									.foreach { messageCount => chat.setupAutoSummaries(size, messageCount) }
							}
						case _ => ()
					}
			}
			else if (input.startsWith("/summarize")) {
				println("Summarizes the chat history so far...\n")
				chat.summarize().foreach { case (schrodinger, completion) =>
					schrodinger.manifest.newTextPointer.addContinuousListener { e => print(e.newValue) }
					completion.waitForResult().log
					println("Summarization finished")
				}
			}
			else if (input.startsWith("/save"))
				StdIn.readNonEmptyLine("Please specify the name of the save file to generate").foreach { fileName =>
					Paths.get(s"Echo/data/test-output/${ fileName.endingWith(".json") }").writeJson(chat).log
						.foreach { path => println(s"Current chat status saved to $path") }
				}
			else if (input.startsWith("/"))
				println("Unrecognized command")
			else if (input.exists { _.isLetter }) {
				val schrodinger = chat.push(input)
				println("\nWaiting for the response...\n")
				schrodinger.manifest.newTextPointer.addContinuousListener { e => print(e.newValue) }
				
				schrodinger.finalResultFuture.waitFor().flatMap { _.wrapped }.log.foreach { reply =>
					Wait(0.2.seconds)
					println(s"\n\n${ reply.statistics }\n")
				}
			}
		}
	}
	
	println("Bye!")
	
	
	// OTHER    ----------------------------
	
	private def requestSystemMessage() =
		requestMultiLineString(
			s"Please specify the system message to use. You can write multiple lines if you start and end with $multiLineIndicator.")
	
	private def requestMultiLineString(prompt: String = ""): Option[String] = {
		StdIn.readNonEmptyLine(prompt).map { firstLine =>
			// Case: Multi-line input
			if (firstLine.startsWith(multiLineIndicator)) {
				// Case: Single-line multi-line input => Removes the multi-line indicators
				if (firstLine.endsWith(multiLineIndicator))
					firstLine.drop(multiLineIndicator.length).dropRight(multiLineIndicator.length)
				// Case: Actual multi-line input => Reads until user ends with """
				else {
					val moreLines = Iterator.continually { StdIn.readLine() }
						.takeTo { _.endsWith(multiLineIndicator) }
						.toOptimizedSeq
					((firstLine.drop(multiLineIndicator.length) +: moreLines.dropRight(1)) :+
						moreLines.last.dropRight(multiLineIndicator.length))
						.dropWhile { _.isEmpty }.dropRightWhile { _.isEmpty }
							.mkString("\n")
				}
			}
			// Case: Single-line input
			else
				firstLine
		}
	}
}
