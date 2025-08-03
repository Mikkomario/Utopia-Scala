package utopia.vault.nosql.factory.row.model

import utopia.flow.generic.factory.FromModelFactoryWithSchema

/**
  * This storable factory uses table structure to validate model before parsing it.
  *
  * Note: New implementations should extend [[utopia.vault.nosql.read.DbRowReader]] and
  * [[utopia.vault.nosql.read.parse.ParseTableModel]] instead of this trait.
  *
  * @author Mikko Hilpinen
  * @since 30.7.2019, v1.3+
  */
trait FromValidatedRowModelFactory[+A] extends FromRowModelFactory[A] with FromModelFactoryWithSchema[A]
{
	override def schema = table.toModelDeclaration
}
