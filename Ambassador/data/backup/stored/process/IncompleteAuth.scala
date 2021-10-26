package utopia.ambassador.model.stored.process

import utopia.ambassador.model.partial.process.IncompleteAuthData
import utopia.vault.model.template.Stored

/**
  * Represents an incomplete authentication attempt that has been recorded to DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class IncompleteAuth(id: Int, data: IncompleteAuthData) extends Stored[IncompleteAuthData, Int]
