package utopia.flow.parse

import utopia.flow.util.Equatable
import utopia.flow.util.StringExtensions._

object NamespacedString
{
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
	  * @return Namespace applying to this string. None if empty namespace is applied.
	  */
	def namespaceOption = namespace.notEmpty
	
	
	// IMPLEMENTED  -----------------------------
	
	override def properties = Vector(local, namespace)
	
	override def toString = s"$namespace:$local"
	
	
	// OTHER    ---------------------------------
	
	def ==(localString: String) = local == localString
	def ~==(localString: String) = local ~== localString
}