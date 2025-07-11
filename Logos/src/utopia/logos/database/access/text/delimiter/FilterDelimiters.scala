package utopia.logos.database.access.text.delimiter

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.DelimiterDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on delimiter properties
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
trait FilterDelimiters[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines delimiter database properties
	  */
	def model = DelimiterDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param text text to target
	  * @return Copy of this access point that only includes delimiters with the specified text
	  */
	def matching(text: String) = filter(model.text.column <=> text)
	
	/**
	  * @param text Targeted text
	  * @return Copy of this access point that only includes delimiters where text is within the specified 
	  * value set
	  */
	def matchingDelimiters(text: Iterable[String]) = filter(model.text.column.in(text))
}

