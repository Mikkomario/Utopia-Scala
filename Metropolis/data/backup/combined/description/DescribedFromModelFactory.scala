package utopia.metropolis.model.combined.description

import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.FromModelFactory
import utopia.metropolis.model.stored.description.DescriptionLinkOld

/**
  * A common trait for factories which parse models into described elements
  * @author Mikko Hilpinen
  * @since 19.7.2020, v1
 *  @tparam A Type of item being described / wrapped (input)
 *  @tparam D Type of described copy of that item (output)
  */
trait DescribedFromModelFactory[A, +D] extends DescribedFactory[A, D] with FromModelFactory[D]
{
	// ABSTRACT	---------------------------
	
	/**
	  * @return A factory used for parsing the item without descriptions included
	  */
	protected def undescribedFactory: FromModelFactory[A]
	
	
	// IMPLEMENTED	-----------------------
	
	override def apply(model: Model[Property]) = undescribedFactory(model).map { item =>
		val descriptions = model("descriptions").getVector.flatMap { _.model }
			.flatMap { DescriptionLinkOld(_).toOption }.toSet
		apply(item, descriptions)
	}
}
