package utopia.vigil.database.access.scope

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.template.Filterable
import utopia.vault.sql.Condition
import utopia.vigil.database.storable.scope.ScopeDbModel

/**
  * Common trait for access points which may be filtered based on scope properties
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait FilterScopes[+Repr] extends Filterable[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines scope database properties
	  */
	def model = ScopeDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param key key to target
	  * @return Copy of this access point that only includes scopes with the specified key
	  */
	def forKey(key: String) = filter(model.key.column <=> key)
	
	/**
	  * @param keys Targeted keys
	  * @return Copy of this access point that only includes scopes where key is within the specified value 
	  * set
	  */
	def forKeys(keys: Iterable[String]) = filter(model.key.column.in(keys))
	
	/**
	  * @param parentId parent id to target
	  * @return Copy of this access point that only includes scopes with the specified parent id
	  */
	def underScope(parentId: Int) = filter(model.parentId.column <=> parentId)
	
	/**
	  * @param parentIds Targeted parent ids
	  * @return Copy of this access point that only includes scopes where parent id is within the specified 
	  * value set
	  */
	def underScopes(parentIds: IterableOnce[Int]) = filter(Condition.indexIn(model.parentId, parentIds))
}

