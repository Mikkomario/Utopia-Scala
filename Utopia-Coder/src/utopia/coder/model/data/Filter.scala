package utopia.coder.model.data

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.util.StringExtensions._

object Filter
{
	/**
	  * @param name A filter text
	  * @return A filter that only accepts items that match exactly the specified string
	  */
	def exact(name: String) = apply(name, mustMatchExactly = true)
}

/**
  * Used for filtering project data
  * @author Mikko Hilpinen
  * @since 14.10.2021, v1.2
  */
case class Filter(text: String, mustMatchExactly: Boolean = false)
{
	/**
	  * @param string A text / string
	  * @return Whether this filter accepts that string / text
	  */
	def apply(string: String) = if (mustMatchExactly) string ~== text else string.containsIgnoreCase(text)
	/**
	  * @param name A name
	  * @return Whether this filter accepts that name
	  */
	def apply(name: Name): Boolean = name.variants.exists(apply)
}