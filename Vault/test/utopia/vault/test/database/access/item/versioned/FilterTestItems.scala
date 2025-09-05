package utopia.vault.test.database.access.item.versioned

import utopia.vault.nosql.view.FilterableView
import utopia.vault.test.database.storable.item.VersionedTestItemDbModel

/**
  * Common trait for access points which may be filtered based on versioned test item properties
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
trait FilterTestItems[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines versioned test item database properties
	  */
	def model = VersionedTestItemDbModel
	
	/**
	  * Copy of this access, limited to currently active test items
	  */
	def active = filter(model.nonDeprecatedCondition)
}

