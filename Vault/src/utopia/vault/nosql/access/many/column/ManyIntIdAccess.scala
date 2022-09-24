package utopia.vault.nosql.access.many.column

import utopia.flow.generic.model.immutable.Value

/**
  * Common trait for access points that are used for reading multiple integer ids from a table / group of tables
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait ManyIntIdAccess extends ManyIdAccess[Int]
{
	override def parseValue(value: Value) = value.getInt
}
