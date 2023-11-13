package utopia.flow.parse.xml

import utopia.flow.operator.equality.{ApproxEquals, EqualsFunction}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.{MaybeEmpty, ScopeUsable}

object Namespace
{
	/**
	  * The empty namespace (i.e. no namespace)
	  */
	implicit val empty: Namespace = Namespace("")
	/**
	  * Approximate equality function for namespaces
	  */
	implicit val approxEquals: EqualsFunction[Namespace] = _ ~== _
	
	/**
	  * Namespace used to define other namespaces
	  */
	val namespaceDeclaration = Namespace("xmlns")
}

/**
  * Represents a namespace in xml context
  * @author Mikko Hilpinen
  * @since 20.6.2022, v1.15.1
  */
case class Namespace(name: String)
	extends ScopeUsable[Namespace] with ApproxEquals[Namespace] with MaybeEmpty[Namespace]
{
	// IMPLEMENTED  ---------------------
	
	override def self = this
	
	/**
	  * @return Whether this is an empty namespace
	  */
	override def isEmpty = name.isEmpty
	
	override def toString = name
	
	/**
	  * Checks whether this namespace resembles the other namespace (case-insensitive).
	  * Empty namespace resembles every namespace.
	  * @param other Another namespace
	  * @return Whether these two namespaces resemble each other
	  */
	override def ~==(other: Namespace) = if (isEmpty || other.isEmpty) true else name ~== other.name
	
	
	// OTHER    -------------------------
	
	/**
	  * @param string A string
	  * @return That string in this namespace
	  */
	def apply(string: String) = NamespacedString(string)(this)
	
	/**
	  * @param other Another namespace
	  * @return This namespace if not empty. Otherwise the other namespace.
	  */
	def orElse(other: => Namespace) = if (isEmpty) other else this
}
