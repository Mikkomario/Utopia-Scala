package utopia.vault.util

import utopia.flow.operator.equality.EqualsFunction
import utopia.vault.model.template.HasId

/**
 * An equals function that compares database ids
 *
 * @author Mikko Hilpinen
 * @since 28.02.2025, v1.20.2
 */
object EqualsById extends EqualsFunction[HasId[Any]]
{
	// IMPLEMENTED  -------------------------
	
	override def apply(a: HasId[Any], b: HasId[Any]): Boolean = a.id == b.id
}
