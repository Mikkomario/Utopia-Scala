package utopia.courier.controller.read

import utopia.courier.model.{Email, EmailAddress}
import utopia.courier.model.read.DeletionRule.NeverDelete
import utopia.courier.model.read.{DeletionRule, FolderPath, ReadSettings}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.LazyTree
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.AutoCloseWrapper
import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.caching.ResettableLazy

import java.io.InputStream
import java.nio.file.Path
import java.util.Properties
import javax.mail.Message.RecipientType
import javax.mail.internet.MimeMessage
import javax.mail._
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Codec
import scala.util.{Failure, Success, Try}

object EmailReader
{
	// ATTRIBUTES   --------------------------
	
	private implicit val defaultCodec: Codec = Codec.UTF8
	
	
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
	  * @param targetFolders Targeted folder or folders. Default = Inbox.
	  * @param maxMessageCount Maximum number of messages processed at this time. Negative if not limited (default).
	  * @param skipMessageCount Number of messages to skip from the beginning (default = 0)
	  * @param deletionRule A rule to apply to deleting messages (default = never delete messages)
	  * @param exc Implicit execution context
	  * @return A future with the read results, when they come available
	  */
	def apply(targetFolders: TargetFolders = TargetFolders.inbox, maxMessageCount: Int = -1, skipMessageCount: Int = 0,
	          deletionRule: DeletionRule = NeverDelete)
	         (implicit exc: ExecutionContext) =
		Future { readBlocking(targetFolders, maxMessageCount, skipMessageCount, deletionRule) }
	
	/**
	  * Asynchronously iterates over read email
	  * @param targetFolders Targeted folder or folders. Default = Inbox.
	  * @param skipMessageCount Number of messages to skip from the beginning (default = 0)
	  * @param deletionRule A rule to apply to deleting messages (default = never delete messages)
	  * @param f Function that accepts an iterator that returns read messages.
	 *                   The specified iterator may contain any number of failures.
	  * @param exc Implicit execution context
	  * @return A future with the read results, when they come available
	  */
	def iterateAsync[B](targetFolders: TargetFolders = TargetFolders.inbox, skipMessageCount: Int = 0,
	                    deletionRule: DeletionRule = NeverDelete)(f: Iterator[Try[A]] => B)
	                   (implicit exc: ExecutionContext) =
		Future { iterateBlocking(targetFolders, skipMessageCount, deletionRule)(f) }
	
	/**
	  * Reads email data from the targeted message folder.
	  * @param targetFolders Targeted folder or folders. Default = Inbox.
	  * @param maxMessageCount Maximum number of read messages (includes those skipped).
	  *                        Negative if not limited (default)
	  * @param skipMessageCount Number of messages to skip from the beginning (default = 0)
	  * @param deletionRule A rule to apply to deleting messages (default = never delete messages)
	  * @return Newly read data. Failure if connection or read setup failed
	  */
	def readBlocking(targetFolders: TargetFolders = TargetFolders.inbox,
	                 maxMessageCount: Int = -1, skipMessageCount: Int = 0,
	                 deletionRule: DeletionRule = NeverDelete) =
		iterateBlocking(targetFolders, skipMessageCount, deletionRule) { _.take(maxMessageCount).toTry }
	
	/**
	  * Reads and processes email data using a message iterator. Message reading is terminated if
	  * reading or parsing fails.
	  * @param targetFolders Targeted folder or folders. Default = Inbox.
	  * @param skipMessageCount Number of messages to skip from the beginning (default = 0)
	  * @param deletionRule A rule to apply to deleting messages (default = never delete messages)
	  * @param f Function that accepts an iterator that returns read messages.
	 *          The specified iterator may contain any number of failures.
	  * @return Newly read data. Failure if connection or read setup failed
	  */
	def iterateBlocking[B](targetFolders: TargetFolders = TargetFolders.inbox,
	                       skipMessageCount: Int = 0,
	                       deletionRule: DeletionRule = NeverDelete)(f: Iterator[Try[A]] => B): B =
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
					// Determines the targeted folders
					val folderTree = LazyTree
						.iterate(Lazy { store.getDefaultFolder -> FolderPath(Vector()) }) { case (folder, path) =>
							folder.list().iterator.map { subFolder => Lazy { subFolder -> (path/subFolder.getName) } }
						}
					// Prepares the folders for reading
					val rawFoldersIterator = targetFolders(folderTree.map { _._2 })
						// Resolves the resulting paths
						.map { path =>
							// Case: Root/default folder was targeted
							if (path.isEmpty)
								Success(store.getDefaultFolder)
							// Case: A non-root folder was targeted => Determines the targeted folder
							else
								path.parts
									// Traverses the tree using the specified path
									.foldLeftIterator(Success(folderTree): Try[LazyTree[(Folder, FolderPath)]]) { (parent, nextPart) =>
										parent.flatMap { p =>
											p.children.find { _.nav._2.name ~== nextPart }
												.toTry { new NoSuchElementException(
													s"$p doesn't contain a folder named $nextPart") }
										}
									}
									// Fails if targeting a non-existing folder
									.takeTo { _.isFailure }
									.last.map { _.nav._1 }
						}
					// Calls the specified function using a prepared message iterator
					// Forms the message processing iterator and gives it to the specified function
					val foldersIterator = new FoldersIterator(rawFoldersIterator, skipMessageCount, deletionRule)
					val iterator = new MessageIterator(
						new RawMessageIterator(foldersIterator, deletionRule, makeBuilder), deletionRule)
					val result = f(iterator)
					// Closes any folder that may have been left open
					foldersIterator.close()
					result
				} }
		// Handles failures by giving them to the specified function
		}.getOrMap { error => f(Iterator.single(Failure(error))) }
	}
	
	private def delete(message: Message) = Try { message.setFlag(Flags.Flag.DELETED, true) }
	
	
	// NESTED   -------------------------------------
	
	private class FoldersIterator(source: Iterator[Try[Folder]], skipMessageCount: Int, deletionRule: DeletionRule)
		extends Iterator[Try[Message]]
	{
		// ATTRIBUTES   ---------------------------
		
		// Number of message skips yet to be performed
		private var remainingSkipCount = skipMessageCount
		// Contains currently open folder and its size
		private var openFolder: Option[(Folder, Int)] = None
		// Index of the next message to read - 1-based
		private var nextMessageIndex = 1
		private var queuedFailures: Iterator[Throwable] = Iterator.empty
		
		override def hasNext: Boolean = queuedFailures.hasNext || openFolder.exists { _._2 > nextMessageIndex } ||
			openNextFolder()
		
		
		// IMPLEMENTED  --------------------------
		
		@tailrec
		override final def next(): Try[Message] = queuedFailures.nextOption() match {
			// Case: A failure was queued => Returns the queued failure
			case Some(error) => Failure(error)
			case None =>
				openFolder.filter { _._2 > nextMessageIndex } match {
					// Case: A folder has been opened (as expected) => Reads the next message from the folder
					case Some((folder, _)) =>
						val message = Try { folder.getMessage(nextMessageIndex) }
						nextMessageIndex += 1
						// Returns the message read result
						message
					// Case: A folder has not been opened (unexpected) => Attempts to open one
					case None =>
						// Case: Opening yielded something to return => Returns that item
						if (openNextFolder())
							next()
						// Case: No more elements to return => Throws
						else
							throw new NoSuchElementException("next() called on an empty iterator")
				}
		}
		
		
		// OTHER    -----------------------------
		
		/**
		 * @return Closes the currently open folder, if applicable.
		 *         Should be called before discarding this iterator, unless it has already been consumed
		 */
		def close(): Try[Unit] = openFolder match {
			case Some((folder, _)) => close(folder)
			case None => Success(())
		}
		
		// Returns if this iterator may return new items afterwards
		@tailrec
		private def openNextFolder(): Boolean = {
			// Closes the previously open folder before opening the next folder
			openFolder.flatMap { case (folder, _) => close(folder).failure } match {
				// Case: Failed to close the folder => Queues the failure
				case Some(failure) =>
					queue(failure)
					true
				// Case: Previous folder closed => Opens the next folder
				case None =>
					source.nextOption() match {
						// Case: Failed to read the next folder => Queues the encountered failure
						case Some(Failure(error)) =>
							queue(error)
							true
						// Case: Next folder is available
						case Some(Success(folder)) =>
							// Attempts to open the folder
							Try { folder.open(if (deletionRule.canDelete) Folder.READ_WRITE else Folder.READ_ONLY) }
								// Counts the messages in the folder
								.flatMap { _ => Try { folder.getMessageCount } } match {
								// Case: Folder successfully opened
								case Success(messageCount) =>
									// Case: All messages in this folder are requested to be skipped
									// => Closes the folder immediately
									if (remainingSkipCount >= messageCount) {
										remainingSkipCount -= messageCount
										close(folder) match {
											// Case: Closing succeeded => Moves to the next folder, if possible
											case Success(_) => openNextFolder()
											// Case: Closing failed => Prepares the closing error as the next element
											case Failure(error) =>
												queue(error)
												true
										}
									}
									// Case: There are messages to iterate => Prepares for the iteration
									else {
										nextMessageIndex = 1 + remainingSkipCount
										remainingSkipCount = 0
										openFolder = Some(folder -> messageCount)
										true
									}
								// Case: Failed to open the folder => Prepares the error as the next element
								case Failure(error) =>
									queue(error)
									true
							}
						// Case: No more folders available
						case None => false
					}
			}
		}
		
		private def close(folder: Folder) = {
			openFolder = None
			Try { folder.close(deletionRule.canDelete) }
		}
		
		private def queue(error: Throwable) = queuedFailures :+= error
	}
	
	private class MessageIterator(source: RawMessageIterator, deletionRule: DeletionRule) extends Iterator[Try[A]]
	{
		// ATTRIBUTES   ---------------------------
		
		// Finds the next non-skipped message & builder
		private val nextMaterials = ResettableLazy {
			source.findMapNext {
				case Success(materials) => materials.map { Success(_) }
				case Failure(error) => Some(Failure(error))
			}
		}
		
		
		// IMPLEMENTED  ---------------------------
		
		override def hasNext = nextMaterials.value.isDefined
		
		override def next() = nextMaterials.pop()
			.getOrElse { throw new NoSuchElementException("Called next() on an empty iterator") } match
		{
			// Case: Next message items could be successfully acquired => Processes message content
			case Success((message, builder)) =>
				// Reads the message content. If the primary content read fails,
				// attempts to get around it by wrapping the message in another instance
				// (Unable to load BODYSTRUCTURE exception workaround)
				val content = Try { message.getContent } match {
					// Case: Access succeeded => continues normally
					case success: Success[AnyRef] => success
					// Case: Initial access failed => Attempts the workaround if applicable
					case failure: Failure[AnyRef] =>
						message match {
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
				// May delete the message afterwards
				// Remembers the result
				// Ignores deletion failures
				if (result.isSuccess) {
					if (deletionRule.shouldDeleteProcessed)
						delete(message)
				} else if (deletionRule.shouldDeleteFailed)
					delete(message)
				result
			// Case: Message parsing failure => this fails also
			case Failure(error) => Failure(error)
		}
		
		
		// OTHER    --------------------------------
		
		private def processContent(content: AnyRef, builder: FromEmailBuilder[A]): Try[Unit] = {
			if (content == null)
				Success(())
			else
				content match {
					case string: String => builder.append(string)
					case stream: InputStream => builder.appendFrom(stream)
					case multiPart: Multipart =>
						(0 until multiPart.getCount).view.tryMap { i => Try { multiPart.getBodyPart(i) } }
							.map { _.filterNot { _ == null } }
							.flatMap { _.tryForeach { part =>
								// Catches exceptions since almost every Java interface method throws
								Try {
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
	
	// Returns Success(None) for messages that are skipped
	private class RawMessageIterator(source: Iterator[Try[Message]], deletionRule: DeletionRule,
	                                 makeBuilder: LazyEmailHeadersView => Option[FromEmailBuilder[A]])
		extends Iterator[Try[Option[(Message, FromEmailBuilder[A])]]]
	{
		// IMPLEMENTED  -----------------------------
		
		override def hasNext = source.hasNext
		
		override def next() = {
			// Reads the next message (may fail)
			source.next().flatMap { message =>
				Try {
					// Reads the header information (lazily)
					def subject = message.getSubject
					def sentTime = message.getSentDate.toInstant
					def sender = Option(message.getFrom) match {
						case Some(senders) => senders.mkString(", ")
						case None => ""
					}
					def recipients =
						Vector(RecipientType.TO, RecipientType.CC, RecipientType.BCC)
							.flatMap { recipientType =>
								Option(message.getRecipients(recipientType))
									.filter { _.length > 0 }
									.map { recipientType -> _.toVector.map { a => EmailAddress(a.toString) } }
							}.toMap
					def replyTo = Option(message.getReplyTo).flatMap { _.headOption }
						.map { a => EmailAddress(a.toString) }
					
					lazy val mimeMessage = message match {
						case m: MimeMessage => Some(m)
						case _ => None
					}
					def messageId = (mimeMessage match {
						case Some(m) => Option(m.getMessageID)
						case None => Option(message.getHeader("Message-ID")).flatMap { _.headOption }
					}) match {
						case Some(mId) => normalizeHeaderValue(mId)
						case None => ""
					}
					def inReplyTo = Option(message.getHeader("In-Reply-To")).flatMap { _.headOption } match {
						case Some(mId) => normalizeHeaderValue(mId)
						case None => ""
					}
					def references = Option(message.getHeader("References")) match {
						case Some(refs) =>
							refs.toVector
								.flatMap { _.split(' ').filter { _.nonEmpty } }
								.map(normalizeHeaderValue).filter { _.nonEmpty }
						case None => Vector.empty
					}
					
					// Creates a builder, if necessary
					makeBuilder(new LazyEmailHeadersView(sender, subject, messageId, sentTime, recipients, inReplyTo,
						references, replyTo))
				} match {
					case Success(builder) =>
						// May delete skipped messages (ignores failures)
						if (builder.isEmpty && deletionRule.shouldDeleteSkipped)
							delete(message)
						Success(builder.map { message -> _ })
					case Failure(error) =>
						// May delete failed messages (ignores failures)
						if (deletionRule.shouldDeleteFailed)
							delete(message)
						Failure(error)
				}
			}
		}
		
		
		// OTHER    --------------------------
		
		private def normalizeHeaderValue(value: String) =
			value.stripControlCharacters.trim.notStartingWith("<").notEndingWith(">")
	}
}
