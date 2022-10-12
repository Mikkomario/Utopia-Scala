package utopia.vault.coder.model.data

import utopia.vault.coder.model.scala.datatype.Reference

/**
  * Contains references concerning a single class
  * @author Mikko Hilpinen
  * @since 14.10.2021, v1.2
  */
case class ClassReferences(model: Reference, data: Reference, factory: Reference, dbModel: Reference,
                           genericAccessTrait: Option[Reference])
