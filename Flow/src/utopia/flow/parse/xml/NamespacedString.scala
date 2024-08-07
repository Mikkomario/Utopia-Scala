package utopia.flow.parse.xml

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.equality.{ApproxEquals, EqualsBy, EqualsFunction}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.util.StringExtensions._

import scala.language.implicitConversions

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
	
	/**
	 * Parses a namespaced string from a string which may or may not contain a namespace prefix.
	 * @param string A string which might contain a namespace prefix (e.g. "ns:local")
	 * @return A namespaced string from that string
	 */
	def parseFrom(string: String) = {
		if (string.contains(':')) {
			val (nsPart, localPart) = string.splitAtFirst(":").toTuple
			apply(localPart)(Namespace(nsPart))
		}
		else
			apply(string)(Namespace.empty)
	}
}

/**
  * Represents a string which includes namespace information
  * @author Mikko Hilpinen
  * @since 31.7.2022, v1.16
  */
case class NamespacedString(local: String)(implicit val namespace: Namespace)
	extends EqualsBy with ApproxEquals[NamespacedString]
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
	
	protected override def equalsProperties: Seq[Any] = Pair(local, namespace)
	
	override def toString = if (hasNamespace) s"$namespace:$local" else local
	
	override def ~==(other: NamespacedString) = (local ~== other.local) && (namespace ~== other.namespace)
	
	
	// OTHER    ---------------------------------
	
	def ==(localString: String) = local == localString
	def ~==(localString: String) = local ~== localString
}