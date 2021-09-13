package utopia.courier.model.read

import utopia.courier.model.{Email, EmailContent, EmailHeaders}
import utopia.flow.time.Today
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringFrom

import java.io.InputStream
import java.nio.file.Path
import scala.collection.immutable.VectorBuilder
import scala.io.Codec
import scala.util.{Success, Try}

/**
  * Used for parsing emails from incoming data
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  * @param headers Message headers
  * @param attachmentsStoreDirectory Directory where attachments will be stored.
  *                                  None if attachments won't be stored (default)
  * @param defaultExtension Extension to apply to the saved attachments when the attachment name doesn't contain
  *                         one (default = "txt")
  * @param codec Implicit encoding expected for the incoming attachments
  */
class EmailBuilder(headers: EmailHeaders, attachmentsStoreDirectory: Option[Path] = None,
                   defaultExtension: String = "txt")(implicit codec: Codec)
	extends FromEmailBuilder[Email]
{
	// ATTRIBUTES   -------------------------------
	
	private val bodyBuilder = new StringBuilder()
	private val attachmentsBuilder = attachmentsStoreDirectory.map { new AttachmentsBuilder(_) }
	
	
	// IMPLEMENTED  -------------------------------
	
	override def append(message: String) =
	{
		bodyBuilder ++= message
		Success()
	}
	
	override def appendFrom(stream: InputStream) = StringFrom.stream(stream).map { bodyBuilder ++= _ }
	
	override def attachFrom(attachmentName: String, stream: InputStream) =
		attachmentsBuilder match {
			case Some(builder) => builder.attachFrom(attachmentName, stream)
			case None => Success(())
		}
	
	override def result() =
	{
		val attachments = attachmentsBuilder match
		{
			case Some(builder) => builder.result()
			case None => Vector()
		}
		Success(Email(headers, EmailContent(bodyBuilder.result(), attachments)))
	}
	
	
	// NESTED   ----------------------------------
	
	private class AttachmentsBuilder(storeDirectory: Path)
	{
		// ATTRIBUTES   --------------------------
		
		private lazy val existingDirectory = storeDirectory.asExistingDirectory
		private val pathsBuilder = new VectorBuilder[Path]()
		
		
		// OTHER    ------------------------------
		
		def attachFrom(attachmentName: String, stream: InputStream): Try[Unit] =
			existingDirectory.flatMap { dir =>
				// Determines the default file name
				val defaultFileName =
				{
					if (attachmentName.isEmpty)
						s"attachment-${Today.toLocalDate.toString}.$defaultExtension"
					else if (attachmentName.contains('.'))
						attachmentName
					else
						s"$attachmentName.$defaultExtension"
				}
				// Writes as a unique file
				(dir/defaultFileName).unique.writeStream(stream).map { pathsBuilder += _ }
			}
		
		def result() = pathsBuilder.result()
	}
}
