package utopia.metropolis.model.combined.description

import utopia.metropolis.model.stored.description.DescriptionLinkOld

import scala.language.implicitConversions

object DescribedFactory
{
	// IMPLICIT ------------------------------
	
	implicit def functionToFactory[A, D](f: (A, Set[DescriptionLinkOld]) => D): DescribedFactory[A, D] = apply(f)
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param f A function behaving like a described factory
	 * @tparam A Type of wrapped item (input)
	 * @tparam D Type of described item (output)
	 * @return That function as a DescribedFactory
	 */
	def apply[A, D](f: (A, Set[DescriptionLinkOld]) => D): DescribedFactory[A, D] = new DescribedFactoryFunction[A, D](f)
	
	
	// NESTED   ------------------------------
	
	private class DescribedFactoryFunction[-A, +D](f: (A, Set[DescriptionLinkOld]) => D) extends DescribedFactory[A, D]
	{
		override def apply(wrapped: A, descriptions: Set[DescriptionLinkOld]) = f(wrapped, descriptions)
	}
}

/**
 * Used for constructing described instances by combining item and description data
 * @author Mikko Hilpinen
 * @since 13.10.2021, v1.2.1
 * @tparam A The type of item being described
 * @tparam D The described version of that item
 */
trait DescribedFactory[-A, +D]
{
	// ABSTRACT -----------------------------
	
	/**
	 * Takes an item and its descriptions and combines them together into a described item
	 * @param wrapped Item being described
	 * @param descriptions Descriptions concerning that item
	 * @return A described copy of that item
	 */
	def apply(wrapped: A, descriptions: Set[DescriptionLinkOld]): D
}
