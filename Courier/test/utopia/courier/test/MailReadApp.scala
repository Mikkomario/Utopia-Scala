package utopia.courier.test

import utopia.courier.controller.read.{EmailBuilder, EmailReader, TargetFolders}
import utopia.courier.model.Authentication
import utopia.courier.model.read.{FolderPath, ImapReadSettings, PopReadSettings, ReadSettings}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._

import java.nio.file.Path
import scala.io.StdIn
import scala.util.{Failure, Success}

/**
  * Used for reading email
  * @author Mikko Hilpinen
  * @since 27.9.2021, v1.0
  */
object MailReadApp extends App
{
	private def ask(question: String = "") = {
		if (question.nonEmpty)
			println(question)
		val str = StdIn.readLine()
		if (str == "exit")
			System.exit(0)
		str
	}
	
	println("Welcome to email reader app")
	println("You can quit at any time by typing 'exit' as input")
	
	private val serverAddress = ask("Please write your email service provider address (host)")
	private val emailAddress = ask("Please write your email address (user)")
	private val password = ask("Please write your (third party app) email password")
	private val auth = Authentication(emailAddress, password)
	private implicit val settings: ReadSettings = {
		val answer = ask("Do you want to use imap (default) or pop3 protocol (i/p)?")
		if (answer.startsWithIgnoreCase("p"))
			PopReadSettings(serverAddress, auth)
		else
			ImapReadSettings(serverAddress, auth)
	}
	
	private val senderFilter = {
		if (ask("Do you want to filter emails by sender?").startsWithIgnoreCase("y"))
			ask("Please write sender email address that is targeted")
		else
			""
	}
	private val subjectFilter = {
		if (ask("Do you want to filter emails by subject?").startsWithIgnoreCase("y"))
			ask("Please write the subject portion that must be included")
		else
			""
	}
	private val attachmentsDirectory = {
		if (ask("Do you want to read attachments?").startsWithIgnoreCase("y"))
		{
			val answer = ask(s"Please write the relative or absolute path to the directory where the attachments will be stored (current directory = ${
				".".toAbsolutePath})")
			if (answer.isEmpty)
				Some(".": Path)
			else
				Some(answer: Path)
		}
		else
			None
	}
	private val reader = EmailReader.filtered { headers =>
		if (senderFilter.nonEmpty && headers.sender.toLowerCase != senderFilter.toLowerCase)
			None
		else if (subjectFilter.isEmpty || headers.subject.containsIgnoreCase(subjectFilter))
			Some(new EmailBuilder(headers.toHeaders, attachmentsDirectory))
		else
			None
	}
	
	private val readCount = ask("How many emails do you want to read? (default = 1)").intOr(1)
	private val targeting = TargetFolders { tree =>
		val foldersStr = ask(s"Which folders do you wish to read?\nAvailable folders: ${
			tree.leavesIterator.map { _.nav }.mkString(", ")}\nHint: Specify a comma-separated list")
		foldersStr.split(',').iterator.map { s => FolderPath(s.trim) }
	}
	
	reader.iterateBlocking(targeting) { iter =>
		iter.take(readCount).foreach {
			case Success(email) =>
				println("\n-------------------------------")
				println(s"Sent by ${email.sender} at ${email.sendTime.toLocalDateTime}")
				println(s"Subject: ${email.subject}")
				if (attachmentsDirectory.nonEmpty && email.attachmentPaths.nonEmpty)
					println(s"${email.attachmentPaths.size} attachments: ${
						email.attachmentPaths.map { _.fileName }.mkString(", ") }")
				println(email.message)
			case Failure(error) =>
				error.printStackTrace()
				println(s"Email reading failed (message: ${error.getMessage}). Please see the error stack above.")
		}
	}
	
	println("Email reading finished")
}
