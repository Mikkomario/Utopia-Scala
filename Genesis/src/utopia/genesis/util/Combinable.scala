package utopia.genesis.util

/**
  * These items can be combined with each other
  * @author Mikko Hilpinen
  * @since 20.6.2019, v2.1
  */
trait Combinable[-N, +Repr]
{
	/**
	  * @param another Another item
	  * @return A combination of thse two items
	  */
	def +(another: N): Repr
}