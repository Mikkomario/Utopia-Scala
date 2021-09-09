package utopia.vault.coder.model.data

import utopia.flow.util.SelfComparable

import scala.language.implicitConversions

object Name
{
	// IMPLICIT -----------------------------
	
	// Implicitly converts strings
	implicit def stringToName(name: String): Name = apply(name)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param singular A singular version of this name
	  * @return Name based on the specified string
	  */
	def apply(singular: String): Name = apply(singular, singular + "s")
}

/**
  * Represents a class / property name
  * @author Mikko Hilpinen
  * @since 4.9.2021, v0.1
  */
case class Name(singular: String, plural: String) extends SelfComparable[Name]
{
	override def toString = singular
	
	override def repr = this
	
	override def compareTo(o: Name) = singular.compareTo(o.singular)
}
