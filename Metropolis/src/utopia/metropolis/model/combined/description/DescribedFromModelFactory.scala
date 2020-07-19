package utopia.metropolis.model.combined.description

import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.FromModelFactory
import utopia.metropolis.model.stored.description.DescriptionLink

/**
  * A common trait for factories which parse models into described elements
  * @author Mikko Hilpinen
  * @since 19.7.2020, v1
  */
trait DescribedFromModelFactory[+D, A] extends FromModelFactory[D]
{
	// ABSTRACT	---------------------------
	
	/**
	  * @return A factory used for parsing the item without descriptions included
	  */
	protected def undescribedFactory: FromModelFactory[A]
	
	/**
	  * Combines an item with its descriptions
	  * @param item An item
	  * @param descriptions Descriptions for the item
	  * @return A described version of the item
	  */
	protected def apply(item: A, descriptions: Set[DescriptionLink]): D
	
	
	// IMPLEMENTED	-----------------------
	
	override def apply(model: Model[Property]) = undescribedFactory(model).map { item =>
		val descriptions = model("descriptions").getVector.flatMap { _.model }
			.flatMap { DescriptionLink(_).toOption }.toSet
		apply(item, descriptions)
	}
}
