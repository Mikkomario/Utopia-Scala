package utopia.vault.coder.model.data

import utopia.coder.model.data.Name
import utopia.coder.model.scala.code.CodePiece

/**
  * A class representing an enumeration value
  * @author Mikko Hilpinen
  * @since 19.8.2022, v1.6.1
  * @param name Name of this enumeration value
  * @param id Identifier of this enumeration value (as code)
  * @param description Documentation / description of this enumeration value
  */
case class EnumerationValue(name: Name, id: CodePiece, description: String = "")
