package utopia.courier.test

import utopia.courier.controller.write.EmailSender
import utopia.courier.model.{Email, EmailContent, EmailHeaders}
import utopia.courier.model.write.{GmailWriteSettings, Recipients}
import utopia.flow.generic.DataType

import scala.io.StdIn
import scala.util.{Failure, Success}

/**
  * A command line test application for sending email through gmail smtp
  * @author Mikko Hilpinen
  * @since 13.9.2021, v0.1
  */
object MailSendApp extends App
{
	DataType.setup()
	
	def ask(question: String = "") = {
		if (question.nonEmpty)
			println(question)
		val str = StdIn.readLine()
		if (str == "exit")
			System.exit(0)
		str
	}
	
	println("Welcome to email sender app")
	println("You can quit at any time by typing 'exit' as input")
	
	val sender = ask("Please write your email address")
	val recipients: Recipients = ask("Please write email recipient(s) (separated by ;)").split(';').toVector
	val subject = ask("Please write message subject")
	val content = ask("Please write message content")
	val email = Email(EmailHeaders.outgoing(sender, recipients, subject), EmailContent(content))
	
	Iterator.continually {
		val pw = ask("Please write your email password to authenticate the sending")
		implicit val writeSettings: GmailWriteSettings = GmailWriteSettings(sender, pw)
		
		println("Attempting to send the mail...")
		EmailSender().sendBlocking(email) match {
			case Success(_) =>
				println("Email sending finished (successfully)")
				false
			case Failure(error) =>
				println(s"Email sending failed due to: ${error.getMessage}")
				ask("Do you want to try again (y/n)?").toLowerCase.startsWith("y")
		}
	}.find { continue => !continue }
	
	println("Closing. Thank you!")
}
