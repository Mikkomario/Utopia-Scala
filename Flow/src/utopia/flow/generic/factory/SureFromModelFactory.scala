package utopia.flow.generic.factory

import utopia.flow.generic.model.template.{ModelLike, Property}

import scala.util.Success

/**
  * A common trait for FromModelFactory variations which always succeed
  * @author Mikko Hilpinen
  * @since 19.2.2022, v1.15
  */
trait SureFromModelFactory[+A] extends FromModelFactory[A]
{
	// ABSTRACT -------------------------------
	
	/**
	  * @param model A model
	  * @return An item parsed from that model
	  */
	def parseFrom(model: ModelLike[Property]): A
	
	
	// IMPLEMENTED  ---------------------------
	
	override def apply(model: ModelLike[Property]) = Success(parseFrom(model))
}
