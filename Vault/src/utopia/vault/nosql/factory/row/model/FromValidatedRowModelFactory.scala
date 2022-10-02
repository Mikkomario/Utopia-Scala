package utopia.vault.nosql.factory.row.model

import utopia.flow.generic.factory.FromModelFactoryWithSchema

/**
  * This storable factory uses table structure to validate model before parsing it
  * @author Mikko Hilpinen
  * @since 30.7.2019, v1.3+
  */
trait FromValidatedRowModelFactory[+A] extends FromRowModelFactory[A] with FromModelFactoryWithSchema[A]
{
	override def schema = table.toModelDeclaration
}
