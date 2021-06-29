package utopia.metropolis.model.combined.description

import utopia.metropolis.model.stored.description.DescriptionLink

/**
  * A common trait for items with descriptions
  * @author Mikko Hilpinen
  * @since 30.6.2021, v1.1
  */
trait Described
{
	/**
	  * @return Descriptions linked with the wrapped item
	  */
	def descriptions: Set[DescriptionLink]
}
