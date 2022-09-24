package utopia.courier.controller.read

import utopia.courier.model.Email
import utopia.courier.model.read.DeletionRule.NeverDelete
import utopia.courier.model.read.{DeletionRule, ReadSettings}
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.AutoCloseWrapper
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.mutable.caching.ResettableLazy

import java.io.InputStream
import java.nio.file.Path
import java.util.Properties
import javax.mail.Message.RecipientType
import javax.mail.internet.MimeMessage
import javax.mail.{Flags, Folder, Message, Multipart, Part, Session}
import scala.collection.immutable.VectorBuilder
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object EmailReader
{
	// COMPUTED ------------------------------
	
	/**
	  * Creates a new default email reader implementation
	  * @param settings Implicit email reading settings to use
	  * @return A new reader which processes emails without attachments
	  */
	def default(implicit settings: ReadSettings) =
		apply { headers => new EmailBuilder(headers.toHeaders) }
	
	
	// OTHER    ------------------------------
	
	/**
	  * Creates a new email reader without subject-based filtering
	  * @param makeBuilder A function for creating a new mail processor. Accepts email subject and email headers.
	  * @param settings Implicit email reading settings to use
	  * @tparam A Type of mail processor output
	  * @return A new mail reader
	  */
	def apply[A](makeBuilder: LazyEmailHeadersView => FromEmailBuilder[A])(implicit settings: ReadSettings) =
		filtered { headers => Some(makeBuilder(headers)) }
	
	/**
	  * Creates a new mail reader that skips some of the incoming messages
	  * @param makeBuilder A function for creating a new mail processor. Accepts email subject and headers.
	  *                    Returns None for messages that should be skipped.
	  * @param settings Implicit email reading settings to use
	  * @tparam A Type of processing result
	  * @return A new email reader
	  */
	def filtered[A](makeBuilder: LazyEmailHeadersView => Option[FromEmailBuilder[A]])(implicit settings: ReadSettings) =
		new EmailReader[A](settings, makeBuilder)
	
	/**
	  * @param attachmentsStoreDirectory Directory where the read attachments will be stored
	  * @param settings Implicit email reading settings
	  * @return A reader that processes emails and includes attachments
	  */
	def defaultWithAttachments(attachmentsStoreDirectory: Path)(implicit settings: ReadSettings) =
		apply { headers => new EmailBuilder(headers.toHeaders, Some(attachmentsStoreDirectory)) }
	
	/**
	  * Creates a new mail reader that filters based on email subject
	  * @param filter A function that accepts email headers and returns whether they should be processed
	  * @param settings Implicit email reading settings to use
	  * @return A new email reader (NB: Ignores attachments)
	  */
	def filteredDefault(filter: LazyEmailHeadersView => Boolean)(implicit settings: ReadSettings): EmailReader[Email] =
		filtered { headers =>
			if (filter(headers))
				Some(new EmailBuilder(headers.toHeaders))
			else
				None
		}
	
	/**
	  * Creates a new mail reader that filters based on email subject and stores attachments
	  * @param filter A function that accepts email headers and returns whether they should be processed
	  * @param settings Implicit email reading settings to use
	  * @return A new email reader
	  */
	def filteredDefaultWithAttachments(attachmentsDirectory: Path)
	                                  (filter: LazyEmailHeadersView => Boolean)
	                                  (implicit settings: ReadSettings): EmailReader[Email] =
		filtered { headers =>
			if (filter(headers))
				Some(new EmailBuilder(headers.toHeaders, Some(attachmentsDirectory)))
			else
				None
		}
}

/**
  * Used for reading emails from an email server
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  */
class EmailReader[A](settings: ReadSettings, makeBuilder: LazyEmailHeadersView => Option[FromEmailBuilder[A]])
{
	/**
	  * Asynchronously reads and parses email
	  * @param folderName Name of the folder to read (default = INBOX)
	  * @param maxMessageCount Maximum number of messages processed at this time. Negative if not limited (default).
	  * @param skipMessageCount Number of messages to skip from the beginning (default = 0)
	  * @param deletionRule A rule to apply to deleting messages (default = never delete messages)
	  * @param exc Implicit execution context
	  * @return A future with the read results, when they come available
	  */
	def apply(folderName: String = "INBOX", maxMessageCount: Int = -1, skipMessageCount: Int = 0,
	          deletionRule: DeletionRule = NeverDelete)(implicit exc: ExecutionContext) =
		Future { readBlocking(folderName, maxMessageCount, skipMessageCount, deletionRule) }
	
	/**
	  * Asynchronously iterates over read email
	  * @param folderName Name of the folder to read (default = INBOX)
	  * @param maxMessageCount Maximum number of messages processed at this time. Negative if not limited (default).
	  * @param skipMessageCount Number of messages to skip from the beginning (default = 0)
	  * @param deletionRule A rule to apply to deleting messages (default = never delete messages)
	  * @param f Function that accepts the message iterator. The iterator will terminate after a failure.
	  * @param exc Implicit execution context
	  * @return A future with the read results, when they come available
	  */
	def iterateAsync[B](folderName: String = "INBOX", maxMessageCount: Int = -1, skipMessageCount: Int = 0,
	                    deletionRule: DeletionRule = NeverDelete)(f: Iterator[Try[A]] => B)
	                   (implicit exc: ExecutionContext) =
		Future { iterateBlocking(folderName, maxMessageCount, skipMessageCount, deletionRule)(f) }
	
	/**
	  * Reads email data from the targeted message folder. Processes read data.
	  * @param folderName Name of the folder to read (default = INBOX)
	  * @param maxMessageCount Maximum number of read messages (includes those skipped).
	  *                        Negative if not limited (default)
	  * @param skipMessageCount Number of messages to skip from the beginning (default = 0)
	  * @param deletionRule A rule to apply to deleting messages (default = never delete messages)
	  * @return Newly read data. Failure if connection or read setup failed
	  */
	def readBlocking(folderName: String = "INBOX", maxMessageCount: Int = -1, skipMessageCount: Int = 0,
	                 deletionRule: DeletionRule = NeverDelete) =
		iterateBlocking(folderName, maxMessageCount, skipMessageCount, deletionRule) {
			_.tryMap { a => a }.map { _.toVector }
		}
	
	/**
	  * Reads and processes email data using a message iterator. Message reading is terminated if
	  * reading or parsing fails.
	  * @param folderName Name of the folder to read (default = INBOX)
	  * @param maxMessageCount Maximum number of read messages (includes those skipped).
	  *                        Negative if not limited (default)
	  * @param skipMessageCount Number of messages to skip from the beginning (default = 0)
	  * @param deletionRule A rule to apply to deleting messages (default = never delete messages)
	  * @param f Function that accepts the message iterator. The iterator will terminate after a failure.
	  * @return Newly read data. Failure if connection or read setup failed
	  */
	def iterateBlocking[B](folderName: String = "INBOX", maxMessageCount: Int = -1, skipMessageCount: Int = 0,
	                       deletionRule: DeletionRule = NeverDelete)(f: Iterator[Try[A]] => B) =
	{
		// Initializes the session
		settings.modify(new Properties).flatMap { properties =>
			val emailSession = Session.getDefaultInstance(properties)
			emailSession.setDebug(false)
			
			// Catches exceptions thrown during connection establishing and folder opening
			Try { emailSession.getStore(settings.storeName) }.map { AutoCloseWrapper(_) { _.close() } }
				.flatMap { _.tryConsumeContent { store =>
					// Connects to the mail server
					store.connect(settings.hostAddress, settings.authentication.user, settings.authentication.password)
					// Reads the targeted mail folder
					AutoCloseWrapper(store.getFolder(folderName)) { _.close(deletionRule.canDelete) }
						.consumeContent { folder =>
							Try {
								// Opens the folder
								folder.open(if (deletionRule.canDelete) Folder.READ_WRITE else Folder.READ_ONLY)
								// Reads targeted messages in the folder
								// (may skip some / limit amount of processed messages)
								val folderSize = folder.getMessageCount
								val maxReadIndex = if (maxMessageCount < 0) folderSize else
									(skipMessageCount + maxMessageCount) min folderSize
								val sourceIterator = {
									if (skipMessageCount >= folderSize)
										Iterator.empty
									else
										(skipMessageCount + 1 to maxReadIndex).iterator
											.map { index => Try { folder.getMessage(index) } }
								}
								// Forms the message processing iterator and gives it to the specified function
								val iterator = new MessageIterator(new RawMessageIterator(sourceIterator, makeBuilder))
								val finalResult = f(iterator)
								// Handles message deletion (ignores failures)
								Try {
									val (processed, skipped, failed) = iterator.popMessages()
									failed.foreach { message => if (deletionRule.shouldDeleteFailed) deleteMessage(message) }
									if (skipped.nonEmpty && deletionRule.shouldDeleteSkipped)
										deleteMessages(skipped)
									if (processed.nonEmpty && deletionRule.shouldDeleteProcessed(failed.isEmpty))
										deleteMessages(processed)
								}
								finalResult
							}
						}
				}.flatten }
		// Handles failures by giving them to the specified function
		}.getOrMap { error => f(Iterator.single(Failure(error))) }
	}
	
	private def deleteMessage(message: Message) = message.setFlag(Flags.Flag.DELETED, true)
	private def deleteMessages(messages: Iterable[Message]) = messages.foreach(deleteMessage)
	
	
	// NESTED   -------------------------------------
	
	private class MessageIterator(source: RawMessageIterator) extends Iterator[Try[A]]
	{
		// ATTRIBUTES   ---------------------------
		
		private val nextMaterials = ResettableLazy {
			source.nextWhere {
				case Success(msg) => msg.isDefined
				case Failure(_) => true
			}.map { _.map { _.get } }
		}
		private val processedMessagesBuilder = new VectorBuilder[Message]()
		
		private var processFailedMessage: Option[Message] = None
		
		
		// IMPLEMENTED  ---------------------------
		
		override def hasNext = processFailedMessage.isEmpty && nextMaterials.value.isDefined
		
		override def next() = nextMaterials.pop().get match {
			// Case: Next message items could be successfully acquired => Processes message content
			case Success((message, builder)) =>
				// Reads the message content. If the primary content read fails,
				// attempts to get around it by wrapping the message in another instance
				// (Unable to load BODYSTRUCTURE exception workaround)
				val content = Try { message.getContent } match
				{
					// Case: Access succeeded => continues normally
					case success: Success[AnyRef] => success
					// Case: Initial access failed => Attempts the workaround if applicable
					case failure: Failure[AnyRef] =>
						message match
						{
							// Case: MimeMessage => Tries the workaround
							case mimeMessage: MimeMessage =>
								// If the workaround fails also, refers back to the original exception
								Try { new MimeMessage(mimeMessage).getContent }.orElse(failure)
							// Case: Not a MimeMessage => Workaround doesn't apply
							case _ => failure
						}
				}
				val result = content
					.flatMap { processContent(_, builder) }
					.flatMap { _ => builder.result() }
				// Remembers the result
				if (result.isSuccess)
					processedMessagesBuilder += message
				else
					processFailedMessage = Some(message)
				result
			// Case: Message parsing failure => this fails also
			case Failure(error) => Failure(error)
		}
		
		
		// OTHER    --------------------------------
		
		def popMessages() = {
			val (skipped, rawFailed) = source.popMessages()
			val failed = processFailedMessage.orElse(rawFailed)
			processFailedMessage = None
			(processedMessagesBuilder.result(), skipped, failed)
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
									if (disposition != null &&
										((disposition ~== Part.ATTACHMENT) || (disposition ~== Part.INLINE)))
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
	
	private class RawMessageIterator(source: Iterator[Try[Message]],
	                              makeBuilder: LazyEmailHeadersView => Option[FromEmailBuilder[A]])
		extends Iterator[Try[Option[(Message, FromEmailBuilder[A])]]]
	{
		// ATTRIBUTES   -----------------------------
		
		private val skippedMessagesBuilder = new VectorBuilder[Message]()
		private var failedMessage: Option[Message] = None
		private var failed = false
		
		
		// IMPLEMENTED  -----------------------------
		
		override def hasNext = !failed && source.hasNext
		
		override def next() =
		{
			// Reads the next message (may fail)
			source.next() match {
				// Case: Message read => processes it
				case Success(message) =>
					Try {
						// Reads the header information (lazily)
						def subject = message.getSubject
						def sentTime = message.getSentDate.toInstant
						def sender = Option(message.getFrom) match
						{
							case Some(senders) => senders.mkString(", ")
							case None => ""
						}
						def recipients =
							Vector(RecipientType.TO, RecipientType.CC, RecipientType.BCC)
								.flatMap { recipientType =>
									Option(message.getRecipients(recipientType))
										.filter { _.length > 0 }
										.map { recipientType -> _.toVector.map { _.toString } }
								}.toMap
						def replyTo = Option(message.getReplyTo).flatMap { _.headOption }.map { _.toString }
							.getOrElse("")
						// Creates a builder, if necessary
						makeBuilder(new LazyEmailHeadersView(sender, subject, sentTime, recipients, replyTo))
					} match {
						case Success(builder) =>
							// Remembers skipped messages
							if (builder.isEmpty)
								skippedMessagesBuilder += message
							Success(builder.map { message -> _ })
						case Failure(error) =>
							// Remembers last failed message
							failedMessage = Some(message)
							failed = true
							Failure(error)
					}
				// Case: Message failed to be read => terminates
				case Failure(error) =>
					failed = true
					Failure(error)
			}
		}
		
		def popMessages() =
		{
			val failed = failedMessage
			failedMessage = None
			skippedMessagesBuilder.result() -> failed
		}
	}
}
