package utopia.courier.model.read

import utopia.courier.model.EmailContent
import utopia.flow.util.Extender

/**
  * Represents an email message being received
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  */
case class IncomingEmail(body: EmailContent, headers: EmailReadHeaders) extends Extender[EmailContent]
{
	override def wrapped = body
}
