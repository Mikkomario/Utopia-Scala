package utopia.flow.operator.combine

/**
  * A common trait for items which can be scaled somehow
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait Scalable[-Scaler, +Repr] extends Any
{
	// ABSTRACT ----------------------------
	
	/**
	  * @param mod A scaling modifier
	  * @return A scaled copy of this instance
	  */
	def *(mod: Scaler): Repr
}
