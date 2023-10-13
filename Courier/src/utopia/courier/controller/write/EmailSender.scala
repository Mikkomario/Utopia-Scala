package utopia.courier.controller.write

import utopia.courier.controller.write.EmailSender.CustomMimeMessage
import utopia.courier.model.write.WriteSettings
import utopia.courier.model.{Authentication, Email}
import utopia.flow.async.process.Loop
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.NotEmpty
import utopia.flow.util.StringExtensions._

import java.util.Date
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart}
import javax.mail.{Authenticator, PasswordAuthentication, Session, Transport}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object EmailSender
{
	// OTHER    -------------------------------
	
	/**
	  * Creates a new email sender
	  * @param defaultMaxSendAttemptsPerMessage Maximum amount of attempts to send a single email by default
	  *                                         (used as defaults in the method .apply(...)) (default = twice)
	  * @param defaultDurationBetweenAttempts Duration to wait between reattempting message sending by default
	  *                                       (used as defaults in the method .apply(...)) (default = 30 seconds)
	  * @param settings Implicit email sending settings to use
	  * @return A new email sender instance
	  */
	def apply(defaultMaxSendAttemptsPerMessage: Int = 2, defaultDurationBetweenAttempts: FiniteDuration = 30.seconds)
	         (implicit settings: WriteSettings) =
		new EmailSender(settings, defaultMaxSendAttemptsPerMessage, defaultDurationBetweenAttempts)
		
	
	// NESTED   ------------------------------
	
	private class CustomMimeMessage(session: Session, messageId: String) extends MimeMessage(session)
	{
		override def updateMessageID() = {
			if (messageId.isEmpty)
				super.updateMessageID()
			else
				setHeader("Message-ID", messageId.startingWith("<").endingWith(">"))
		}
	}
}

/**
  * Used for sending email
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  * @param settings Settings used by this sender
  * @param defaultMaxSendAttemptsPerMessage Maximum amount of attempts to send a single email by default
  *                                         (used as defaults in the method .apply(...)) (default = twice)
  * @param defaultDurationBetweenAttempts Duration to wait between reattempting message sending by default
  *                                       (used as defaults in the method .apply(...)) (default = 30 seconds)
  */
class EmailSender(settings: WriteSettings, defaultMaxSendAttemptsPerMessage: Int = 2,
                  defaultDurationBetweenAttempts: FiniteDuration = 30.seconds)
{
	// ATTRIBUTES   ---------------------------------
	
	private val authenticator = settings.authentication.map { new CustomAuthenticator(_) }
	
	
	// OTHER    --------------------------------------
	
	/**
	  * Sends an email message asynchronously, possibly making multiple attempts
	  * @param email Email message to send
	  * @param maxAttempts Maximum number of attempts to send the message (default = instance default)
	  * @param durationBetweenAttempts Wait duration between repeated send attempts (default = instance default)
	  * @param exc Implicit execution context
	  * @return Future of the send completion, including whether the sending failed or succeeded. Please note that
	  *         a success doesn't necessarily mean that the message was successfully delivered.
	  */
	def send(email: Email, maxAttempts: Int = defaultMaxSendAttemptsPerMessage,
	          durationBetweenAttempts: FiniteDuration = defaultDurationBetweenAttempts)
	         (implicit exc: ExecutionContext) =
		Loop.tryRepeatedly(durationBetweenAttempts, maxAttempts) { sendBlocking(email) }
	
	/**
	  * Sends an email in the current thread, blocking for the duration of the operation
	  * @param email Email to send
	  * @return Success or failure. Please note that receiving a success doesn't necessarily mean that the message
	  *         was successfully delivered.
	  */
	def sendBlocking(email: Email) =
		Try {
			// Sets class loader to avoid UnsupportedDataTypeException
			// See: https://stackoverflow.com/questions/21856211/javax-activation-unsupporteddatatypeexception-no-object-dch-for-mime-type-multi
			Thread.currentThread().setContextClassLoader(getClass.getClassLoader) }
		// Next modifies system properties according to the settings used
		.flatMap { _ => settings.modify(System.getProperties) }
		.flatMap { properties =>
			Try {
				// Next creates the session and the message and then sends the message
				val session = Session.getInstance(properties, authenticator.orNull)
				session.setDebug(false)
				
				val message = new CustomMimeMessage(session, email.messageId)
				message.setFrom(email.headers.sender)
				message.addHeader("Reply-To", email.headers.replyTo)
				message.addHeader("X-Mailer", "JavaMail API")
				message.setSentDate(new Date())
				message.setSubject(email.subject)
				email.headers.recipients.foreach { case (recipient, recipientType) =>
						message.addRecipient(recipientType, new InternetAddress(recipient))
				}
				email.inReplyTo.notEmpty.foreach { id => message.setHeader("In-Reply-To", s"<$id>") }
				NotEmpty((email.references ++ email.inReplyTo.notEmpty).distinct).foreach { refs =>
					message.setHeader("References", refs.map { id => s"<$id>" }.mkString(" "))
				}
				val textPart = email.message.notEmpty.map { text =>
					val part = new MimeBodyPart()
					part.setContent(text, "text/html; charset=utf-8")
					part
				}
				val attachmentParts = email.attachmentPaths.map { path =>
					val part = new MimeBodyPart()
					part.attachFile(path.toFile)
					part
				}
				val multiPart = new MimeMultipart()
				(textPart ++ attachmentParts).foreach(multiPart.addBodyPart)
				message.setContent(multiPart)
				
				Transport.send(message)
			}
		}
	
	private class CustomAuthenticator(auth: Authentication) extends Authenticator
	{
		val pwAuth = new PasswordAuthentication(auth.user, auth.password)
		
		override def getPasswordAuthentication = pwAuth
	}
}
