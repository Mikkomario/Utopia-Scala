package utopia.vault.model.immutable.access

import utopia.flow.datastructure.immutable.Value

/**
 * This implementation of SingleIdAccess supports int ids
 * @author Mikko Hilpinen
 * @since 30.7.2019, v1.3+
 */
@deprecated("Replaced with utopia.vault.nosql.access.IdAccess", "v1.4")
trait IntIdAccess extends IdAccess[Int]
{
	override protected def valueToId(value: Value) = value.getInt
}
