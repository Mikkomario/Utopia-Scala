package utopia.courier.controller

import utopia.courier.model.read.DeletionRule.NeverDelete
import utopia.courier.model.read.{DeletionRule, EmailReadHeaders, FromEmailBuilder, ReadSettings}
import utopia.flow.util.AutoClose._
import utopia.flow.util.AutoCloseWrapper
import utopia.flow.util.CollectionExtensions._

import java.io.InputStream
import java.util.Properties
import javax.mail.{Flags, Folder, Message, Multipart, Part, Session}
import scala.collection.immutable.VectorBuilder
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

/**
  * Used for reading emails from an email server
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  */
class EmailReader[A](settings: ReadSettings, makeBuilder: (String, EmailReadHeaders) => Option[FromEmailBuilder[A]])
{
	/**
	  * Asynchronously reads and parses email
	  * @param folderName Name of the folder to read (default = INBOX)
	  * @param deletionRule A rule to apply to deleting messages (default = never delete messages)
	  * @param exc Implicit execution context
	  * @return A future with the read results, when they come available
	  */
	def apply(folderName: String = "INBOX", deletionRule: DeletionRule = NeverDelete)(implicit exc: ExecutionContext) =
		Future { readBlocking(folderName, deletionRule) }
	
	/**
	  * Reads email data from the targeted message folder. Processes read data.
	  * @param folderName Name of the folder to read (default = INBOX)
	  * @param deletionRule A rule to apply to deleting messages (default = never delete messages)
	  * @return Newly read data. Failure if connection or read setup failed
	  */
	def readBlocking(folderName: String = "INBOX", deletionRule: DeletionRule = NeverDelete) =
	{
		// Initializes the session
		settings.modify(new Properties).flatMap { properties =>
			val emailSession = Session.getDefaultInstance(properties)
			emailSession.setDebug(false)
			
			// Catches exceptions thrown during connection establishing and folder opening
			Try {
				AutoCloseWrapper(emailSession.getStore(settings.storeName)) { _.close() }.consumeContent { store =>
					// Connects to the mail server
					store.connect(settings.hostAddress, settings.authentication.user, settings.authentication.password)
					// Reads the targeted mail folder
					AutoCloseWrapper(store.getFolder(folderName)) { _.close(deletionRule.canDelete) }
						.consumeContent { folder =>
							// Records the messages so that they can be deleted when folder closes
							var lastInitiatedMessage: Option[Message] = None
							// Needs to catch thrown exceptions to handle deletion correctly
							val result = Try {
								val readMessagesBuilder = new VectorBuilder[Message]()
								
								// Opens the inbox
								folder.open(if (deletionRule.canDelete) Folder.READ_WRITE else Folder.READ_ONLY)
								val resultsBuilder = new VectorBuilder[A]()
								// Reads and parses all messages in the folder
								folder.getMessages.tryForeach { message =>
									lastInitiatedMessage = Some(message)
									val subject = message.getSubject
									val sentTime = message.getSentDate.toInstant
									val sender = Option(message.getFrom) match
									{
										case Some(senders) => senders.mkString(", ")
										case None => ""
									}
									/* Kept here as a reference, in case recipients should be added
									val recipients = Option(message.getAllRecipients) match
									{
										case Some(recipients) => recipients.view.map { _.toString }.toVector
										case None => Vector()
									}*/
									val headers = EmailReadHeaders(sender, sentTime)
									makeBuilder(subject, headers).tryForeach { builder =>
										// Parses body and attachments
										processContent(message.getContent, builder).flatMap { _ =>
											builder.result().map { resultsBuilder += _ }
										}
									}.map { _ => readMessagesBuilder += message }
								}.flatMap { _ =>
									// All messages were read successfully. May delete them afterwards.
									if (deletionRule.shouldDelete(wasSuccess = true))
										// Deletion may also throw, however
										Try {
											readMessagesBuilder.result()
												.foreach { _.setFlag(Flags.Flag.DELETED, true) }
											resultsBuilder.result()
										}
									else
										Success(resultsBuilder.result())
								}
							}.flatten
							// On failure, may delete the last processed message
							if (result.isFailure && deletionRule.shouldDelete(wasSuccess = false))
								Try { lastInitiatedMessage.foreach { _.setFlag(Flags.Flag.DELETED, true) } }
							result
						}
				}
			}.flatten
		}
	}
	
	private def processContent(content: AnyRef, builder: FromEmailBuilder[A]): Try[Unit] =
	{
		if (content == null)
			Success(())
		else
			content match
			{
				case string: String => builder.append(string)
				case stream: InputStream => builder.appendFrom(stream)
				case multiPart: Multipart =>
					(0 until multiPart.getCount).view.tryMap { i => Try { multiPart.getBodyPart(i) } }
						.map { _.filterNot { _ == null } }
						.flatMap { _.tryForeach { part =>
							// Catches exceptions since almost every Java interface method throws
							Try
							{
								// Handles attachments or other part content
								val disposition = part.getDisposition
								// Case: Attachment => Attaches it using the builder
								if (disposition != null && (disposition == Part.ATTACHMENT || disposition == Part.INLINE))
								{
									// Doesn't know the stream encoding, unfortunately
									part.getInputStream.consume { stream =>
										builder.attachFrom(Option(part.getFileName).getOrElse(""), stream)
									}
								}
								// Case: Other content => Uses recursion to process that one
								else
									processContent(part.getContent, builder)
							}.flatten
						} }
			}
	}
}
