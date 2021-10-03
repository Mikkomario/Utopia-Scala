package utopia.courier.model

import utopia.flow.util.Extender

/**
  * Represents a full email
  * @author Mikko Hilpinen
  * @since 13.9.2021, v0.1
  */
case class Email(headers: EmailHeaders, content: EmailContent) extends Extender[EmailHeaders]
{
	// COMPUTED ------------------------------
	
	/**
	  * @return Message content text
	  */
	def message = content.message
	/**
	  * @return Message attachments
	  */
	def attachmentPaths = content.attachmentPaths
	
	
	// IMPLEMENTED  --------------------------
	
	override def wrapped = headers
}
