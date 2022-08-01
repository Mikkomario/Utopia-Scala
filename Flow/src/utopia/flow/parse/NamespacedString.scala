package utopia.flow.parse

import utopia.flow.util.{EqualsFunction, Equatable}
import utopia.flow.util.StringExtensions._

object NamespacedString
{
	/**
	  * An equality function that checks whether the two strings match each other. The check is case-insensitive and
	  * an empty namespace is considered to match with every other namespace.
	  */
	implicit val approxEquals: EqualsFunction[NamespacedString] = _ ~== _
	
	/**
	  * @param local Local part of this string
	  * @param namespace Namespace applied to the local string
	  * @return A namespaced string
	  */
	implicit def autoConvert(local: String)(implicit namespace: Namespace): NamespacedString = apply(local)(namespace)
}

/**
  * Represents a string which includes namespace information
  * @author Mikko Hilpinen
  * @since 31.7.2022, v1.16
  */
case class NamespacedString(local: String)(implicit val namespace: Namespace) extends Equatable
{
	// COMPUTED ---------------------------------
	
	/**
	  * @return Whether this string has a namespace definition
	  */
	def hasNamespace = namespace.nonEmpty
	/**
	  * @return Whether this string doesn't have an associated namespace
	  */
	def hasNoNamespace = namespace.isEmpty
	
	/**
	  * @return Namespace applying to this string. None if empty namespace is applied.
	  */
	def namespaceOption = namespace.notEmpty
	
	
	// IMPLEMENTED  -----------------------------
	
	override def properties = Vector(local, namespace)
	
	override def toString = if (hasNamespace) s"$namespace:$local" else local
	
	
	// OTHER    ---------------------------------
	
	def ==(localString: String) = local == localString
	def ~==(localString: String) = local ~== localString
	def ~==(other: NamespacedString) = (local ~== other.local) && (namespace ~== other.namespace)
}