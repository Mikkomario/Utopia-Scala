package utopia.vault.nosql.targeting

import utopia.flow.operator.Identity

object Targeting
{
	// OTHER    ------------------------
	
	def map[O, R, V](original: Targeting[O, V])(f: O => R) =
		mapResultsAndValues(original)(f)(Identity)
	def mapValues[A, OV, RV](original: Targeting[A, OV])(f: OV => RV) =
		mapResultsAndValues(original)(Identity)(f)
	
	def mapResultsAndValues[O, OV, R, RV](original: Targeting[O, OV])(mapResult: O => R)
	                                     (mapValue: OV => RV): Targeting[R, RV] =
		new Wrapper(original, mapResult, mapValue)
	
	
	// NESTED   ------------------------
	
	private class Wrapper[O, OV, +R, +RV](override val wrapped: Targeting[O, OV], f: O => R, fv: OV => RV)
		extends Targeting[R, RV] with TargetingWrapper[Targeting[O, OV], O, OV, R, RV, Targeting[R, RV]]
	{
		override protected def self: Targeting[R, RV] = this
		
		override protected def wrapResult(result: O): R = f(result)
		override protected def wrapValue(value: OV): RV = fv(value)
		
		override protected def wrap(newTarget: Targeting[O, OV]): Targeting[R, RV] = new Wrapper(newTarget, f, fv)
	}
}

/**
  * Common trait for access points that can be filtered and/or extended
  * @author Mikko Hilpinen
  * @since 18.05.2025, v1.21
  */
trait Targeting[+A, +Val] extends TargetingLike[A, Val, Targeting[A, Val]]
{
	/**
	  * @param f A mapping function applied to this access point's results
	  * @tparam B Type of mapping results
	  * @return An access point that yields mapped results
	  */
	def mapResult[B](f: A => B): Targeting[B, Val] = Targeting.map(this)(f)
	/**
	  * @param f A mapping function applied to pulled values
	  * @tparam V2 Type of mapping results
	  * @return An access point that yields mapped values
	  */
	def mapValues[V2](f: Val => V2): Targeting[A, V2] = Targeting.mapValues(this)(f)
	def mapResultAndValues[B, V2](mapResult: A => B)(mapValue: Val => V2) =
		Targeting.mapResultsAndValues(this)(mapResult)(mapValue)
}