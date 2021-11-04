package utopia.ambassador.model.stored.process

import utopia.ambassador.model.partial.process.IncompleteAuthLoginData
import utopia.vault.model.template.Stored

/**
  * Represents a recorded incomplete authentication case closure with login
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class IncompleteAuthLogin(id: Int, data: IncompleteAuthLoginData) extends Stored[IncompleteAuthLoginData, Int]
