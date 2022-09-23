package utopia.metropolis.model.stored.language

import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.StyledModelConvertible
import utopia.metropolis.model.combined.description.LinkedDescription
import utopia.metropolis.model.combined.language.DescribedLanguageFamiliarity
import utopia.metropolis.model.partial.language.LanguageFamiliarityData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents a LanguageFamiliarity that has already been stored in the database
  * @param id id of this LanguageFamiliarity in the database
  * @param data Wrapped LanguageFamiliarity data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class LanguageFamiliarity(id: Int, data: LanguageFamiliarityData) 
	extends StoredModelConvertible[LanguageFamiliarityData] with StyledModelConvertible
{
	// IMPLEMENTED  ----------------------
	
	override def toSimpleModel = Model(Vector("id" -> id, "order_index" -> data.orderIndex))
	
	
	// OTHER    --------------------------
	
	/**
	  * @param descriptions Descriptions to apply to this language familiarity
	  * @return A copy of this familiarity with those descriptions included
	  */
	def withDescriptions(descriptions: Set[LinkedDescription]) = DescribedLanguageFamiliarity(this, descriptions)
}
