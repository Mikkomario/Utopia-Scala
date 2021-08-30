package utopia.citadel.coder.model.scala

/**
  * Represents an imported external class or object etc.
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param parentPath Path leading to the imported item. E.g. "utopia.citadel.coder.model.scala"
  * @param target Name of the imported item. E.g. "Reference"
  */
case class Reference(parentPath: String, target: String)
