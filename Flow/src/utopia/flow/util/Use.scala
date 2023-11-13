package utopia.flow.util

import utopia.flow.operator.ScopeUsable

/**
  * An utility object that provides scope-specific access to a certain value.
  * This may be useful in situations where you want to use an implicit value, but want to limit the
  * implicit access to a certain subsection of the current scope.
  *
  * For classes that are often used in this manner, please consider extending [[ScopeUsable]] instead.
  * If a finally-statement should be applied after the use of the specified item, please consider using
  * [[utopia.flow.parse.AutoClose]] or [[utopia.flow.parse.AutoCloseWrapper]] instead.
  *
  * @author Mikko Hilpinen
  * @since 5.11.2023, v2.3
  */
object Use
{
	/**
	  * Provides the specified item for the specified scope
	  * @param item An item to use within the specified scope
	  * @param f A function within which the specified item should be used
	  * @tparam A Type of the specified item
	  * @tparam R Type of the return value of the specified function
	  * @return The return value of the specified function
	  */
	def apply[A, R](item: A)(f: A => R): R = f(item)
}
