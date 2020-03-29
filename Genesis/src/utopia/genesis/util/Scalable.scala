package utopia.genesis.util

/**
  * Scalable instances can be scaled linearly
  * @author Mikko Hilpinen
  * @since 20.6.2019, v2.1+
  */
trait Scalable[+Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param mod A scaling modifier
	  * @return A scaled version of this instance
	  */
	def *(mod: Double): Repr
	
	
	// OPERATORS	----------------
	
	/**
	  * @param div A divider
	  * @return A divided version of this instance
	  */
	def /(div: Double) = *(1/div)
	
	/**
	  * @return A negated / inverted version of this scalable element
	  */
	def unary_- = this * -1
}
