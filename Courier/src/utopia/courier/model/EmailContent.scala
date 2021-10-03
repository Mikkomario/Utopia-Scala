package utopia.courier.model

import java.nio.file.Path

/**
  * Represents email content that is sent or received, not including headers
  * @author Mikko Hilpinen
  * @since 10.9.2021, v0.1
  * @param message Html message content of this email
  * @param attachmentPaths Paths to the files that are linked as attachments for this email
  */
case class EmailContent(message: String = "", attachmentPaths: Vector[Path] = Vector())
