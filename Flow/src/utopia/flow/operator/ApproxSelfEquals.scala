package utopia.flow.operator

/**
  * A common trait for items that can be tested against their own type of equality
  * @author Mikko Hilpinen
  * @since 7.10.2022, v2.0
  */
trait ApproxSelfEquals[Repr] extends ApproxEquals[Repr]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return This instance
	  */
	def self: Repr
	
	/**
	  * @return An equals function to use for comparing instances of this class
	  */
	implicit def equalsFunction: EqualsFunction[Repr]
	
	
	// IMPLEMENTED  -------------------
	
	override def ~==(other: Repr) = equalsFunction(self, other)
}
