package utopia.vault.nosql.factory

import utopia.flow.generic.FromModelFactoryWithSchema

/**
  * This storable factory uses table structure to validate model before parsing it
  * @author Mikko Hilpinen
  * @since 30.7.2019, v1.3+
  */
trait StorableFactoryWithValidation[+A] extends StorableFactory[A] with FromModelFactoryWithSchema[A]
{
	override def schema = table.requirementDeclaration
}
