package utopia.vault.nosql.access.single.id

import utopia.flow.datastructure.immutable.Value

/**
  * Trait for access points that access individual integer ids from a table or a group of tables
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait SingleIntIdAccess extends SingleIdAccess[Int]
{
	override def valueToId(value: Value) = value.int
}
