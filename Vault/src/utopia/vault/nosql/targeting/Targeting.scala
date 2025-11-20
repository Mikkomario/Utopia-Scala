package utopia.vault.nosql.targeting

object Targeting
{
	// OTHER    ------------------------
	
	def map[O, R, V, VV](original: Targeting[O, V, VV])(f: O => R): Targeting[R, V, VV] =
		new Wrapper(original, f)
	
	
	// NESTED   ------------------------
	
	private class Wrapper[O, V, VV, +R](override val wrapped: Targeting[O, V, VV], f: O => R)
		extends Targeting[R, V, VV] with TargetingWrapper[Targeting[O, V, VV], O, V, VV, R, V, VV, Targeting[R, V, VV]]
	{
		override def self: Targeting[R, V, VV] = this
		
		override protected def wrapResult(result: O): R = f(result)
		override protected def wrapValue(value: V) = value
		override protected def wrapValues(values: VV): VV = values
		
		override protected def wrap(newTarget: Targeting[O, V, VV]): Targeting[R, V, VV] = new Wrapper(newTarget, f)
	}
}

/**
  * Common trait for access points that can be filtered and/or extended
  * @author Mikko Hilpinen
  * @since 18.05.2025, v1.21
  */
trait Targeting[+A, +Val, +Vals] extends TargetingLike[A, Val, Vals, Targeting[A, Val, Vals]]
{
	/**
	  * @param f A mapping function applied to this access point's results
	  * @tparam B Type of mapping results
	  * @return An access point that yields mapped results
	  */
	def mapResult[B](f: A => B): Targeting[B, Val, Vals] = Targeting.map(this)(f)
	/*
	/**
	  * @param f A mapping function applied to pulled values
	  * @tparam V2 Type of mapping results
	  * @return An access point that yields mapped values
	  */
	def mapValues[V2](f: Val => V2): Targeting[A, V2] = Targeting.mapValues(this)(f)
	/**
	  * @param mapResult A mapping function applied to this access point's results
	  * @param mapValue A mapping function applied to pulled values
	  * @tparam B Type of result-mapping results
	  * @tparam V2 Type of value-mapping results
	  * @return An access point that yields mapped results and values
	  */
	def mapResultAndValues[B, V2](mapResult: A => B)(mapValue: Val => V2) =
		Targeting.mapResultsAndValues(this)(mapResult)(mapValue)
	 */
}