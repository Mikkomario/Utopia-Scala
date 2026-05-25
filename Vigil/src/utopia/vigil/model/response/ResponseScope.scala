package utopia.vigil.model.response

import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.vault.store.HasId

/**
 * A response model representing a single authentication scope
 * @param id ID of this scope
 * @param name Name / key of this scope
 * @param grants Scopes granted by this scope
 * @author Mikko Hilpinen
 * @since 24.05.2026, v0.1
 */
case class ResponseScope(id: Int, name: String, grants: Seq[ResponseScope] = Empty)
	extends HasId[Int] with ModelConvertible
{
	// IMPLEMENTED  ----------------------
	
	override def toModel: Model = Model.from("id" -> id, "name" -> name, "grants" -> grants)
	
	
	// OTHER    --------------------------
	
	/**
	 * @param accessibleIds IDs of the accessible scopes
	 * @return A model representation of this scope including "accessible" properties
	 */
	def toModelWithAccessibleScopeIds(accessibleIds: Set[Int]): Model =
		Model.from("id" -> id, "name" -> name, "accessible" -> accessibleIds.contains(id),
			"grants" -> grants.map { _.toModelWithAccessibleScopeIds(accessibleIds) })
}