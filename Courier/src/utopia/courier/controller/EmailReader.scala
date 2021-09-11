package utopia.courier.controller

import utopia.courier.model.read.DeletionRule.NeverDelete
import utopia.courier.model.read.{DeletionRule, EmailBuilder, EmailReadHeaders, FromEmailBuilder, IncomingEmail, ReadSettings}
import utopia.flow.datastructure.mutable.ResettableLazy
import utopia.flow.util.AutoClose._
import utopia.flow.util.AutoCloseWrapper
import utopia.flow.util.CollectionExtensions._

import java.io.InputStream
import java.util.Properties
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
	  * @return A new reader
	  */
	def default(implicit settings: ReadSettings) = apply { new EmailBuilder(_, _) }
	
	
	// OTHER    ------------------------------
	
	/**
	  * Creates a new email reader without subject-based filtering
	  * @param makeBuilder A function for creating a new mail processor. Accepts email subject and email headers.
	  * @param settings Implicit email reading settings to use
	  * @tparam A Type of mail processor output
	  * @return A new mail reader
	  */
	def apply[A](makeBuilder: (String, EmailReadHeaders) => FromEmailBuilder[A])(implicit settings: ReadSettings) =
		filtered { (sub, headers) => Some(makeBuilder(sub, headers)) }
	
	/**
	  * Creates a new mail reader that skips some of the incoming messages
	  * @param makeBuilder A function for creating a new mail processor. Accepts email subject and headers.
	  *                    Returns None for messages that should be skipped.
	  * @param settings Implicit email reading settings to use
	  * @tparam A Type of processing result
	  * @return A new email reader
	  */
	def filtered[A](makeBuilder: (String, EmailReadHeaders) => Option[FromEmailBuilder[A]])
	               (implicit settings: ReadSettings) =
		new EmailReader[A](settings, makeBuilder)
	
	/**
	  * Creates a new mail reader that filters based on email subject
	  * @param subjectFilter A function that accepts email subjects and returns whether they should be processed
	  * @param settings Implicit email reading settings to use
	  * @return A new email reader
	  */
	def filteredDefault(subjectFilter: String => Boolean)(implicit settings: ReadSettings): EmailReader[IncomingEmail] =
		filtered { (sub, headers) =>
			if (subjectFilter(sub))
				Some(new EmailBuilder(sub, headers))
			else
				None
		}
}

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
								val sourceIterator =
								{
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
	
	private class MessageIterator(rawSource: RawMessageIterator) extends Iterator[Try[A]]
	{
		// ATTRIBUTES   ---------------------------
		
		private val source = rawSource.pollable
		private val nextMaterials = ResettableLazy {
			source.pollToNextWhere {
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
				val result = Try { message.getContent }
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
			val (skipped, rawFailed) = rawSource.popMessages()
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
	
	private class RawMessageIterator(source: Iterator[Try[Message]],
	                              makeBuilder: (String, EmailReadHeaders) => Option[FromEmailBuilder[A]])
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
						makeBuilder(subject, EmailReadHeaders(sender, sentTime))
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
