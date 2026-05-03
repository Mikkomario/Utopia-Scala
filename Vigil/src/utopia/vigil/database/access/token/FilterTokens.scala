package utopia.vigil.database.access.token

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.{DeprecatableView, TimelineView}
import utopia.vault.sql.Condition
import utopia.vigil.database.storable.token.TokenDbModel

/**
  * Common trait for access points which may be filtered based on token properties
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait FilterTokens[+Repr] extends TimelineView[Repr] with DeprecatableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines token database properties
	  */
	def model = TokenDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def timestampColumn = model.created
	
	
	// OTHER	--------------------
	
	/**
	  * @param parentId parent id to target
	  * @return Copy of this access point that only includes tokens with the specified parent id
	  */
	def createdUsing(parentId: Int) = filter(model.parentId.column <=> parentId)
	/**
	  * @param parentIds Targeted parent ids
	  * @return Copy of this access point that only includes tokens where parent id is within the specified 
	  * value set
	  */
	def createdUsingTokens(parentIds: IterableOnce[Int]) = filter(Condition.indexIn(model.parentId, parentIds))
	
	/**
	  * @param templateId template id to target
	  * @return Copy of this access point that only includes tokens with the specified template id
	  */
	def fromTemplate(templateId: Int) = filter(model.templateId.column <=> templateId)
	/**
	  * @param templateIds Targeted template ids
	  * @return Copy of this access point that only includes tokens where template id is within the specified 
	  * value set
	  */
	def fromTemplates(templateIds: IterableOnce[Int]) = filter(Condition.indexIn(model.templateId, 
		templateIds))
	
	/**
	  * @param hash hash to target
	  * @return Copy of this access point that only includes tokens with the specified hash
	  */
	def withHash(hash: String) = filter(model.hash.column <=> hash)
	/**
	  * @param hashes Targeted hashes
	  * @return Copy of this access point that only includes tokens where hash is within the specified value 
	  * set
	  */
	def withHashes(hashes: Iterable[String]) = filter(model.hash.column.in(hashes))
}

