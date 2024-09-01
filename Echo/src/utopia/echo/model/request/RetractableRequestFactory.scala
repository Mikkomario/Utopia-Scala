package utopia.echo.model.request

import utopia.flow.view.immutable.View

/**
  * Common trait for request factories which support custom deprecation conditions.
  * NB: This trait doesn't actually define the factory function.
  * @tparam Repr Type of this factory
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
@deprecated("Deprecated for removal", "v1.1")
trait RetractableRequestFactory[+Repr]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return A condition that, if met, causes the generated request to be retracted, unless it has been sent already.
	  *         None if no such condition exists.
	  */
	protected def deprecationCondition: Option[View[Boolean]]
	
	/**
	  * @param condition A view which contains true if the request should be retracted,
	  *                  unless it has already been sent.
	  * @return Copy of this factory which uses the specified deprecation condition
	  *         instead of the currently specified condition.
	  */
	def withDeprecationCondition(condition: View[Boolean]): Repr
	
	
	// OTHER    ------------------------
	
	/**
	  * @param condition An (additional) deprecation condition to apply.
	  *                  Specified as a call-by-name value which yields true if the request should be retracted
	  *                  (unless it has already been sent).
	  * @return Copy of this factory which applies the specified deprecation condition.
	  *         If this factory already specified a deprecation condition,
	  *         either of these conditions may be met for the retraction to occur.
	  */
	def deprecatingIf(condition: => Boolean) = deprecationCondition match {
		case Some(existingCondition) => withDeprecationCondition(View { existingCondition.value || condition })
		case None => withDeprecationCondition(View(condition))
	}
}
