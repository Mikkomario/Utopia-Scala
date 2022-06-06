package utopia.vault.coder.model.data

import utopia.flow.util.SelfComparable
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.enumeration.NamingConvention
import utopia.vault.coder.model.enumeration.NamingConvention.Text

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
	def apply(singular: String): Name = apply(singular, NamingConvention.of(singular))
	
	/**
	  * @param singular A singular version of this name
	  * @param style Style in which this name is given
	  * @return Name based on the specified string
	  */
	def apply(singular: String, style: NamingConvention): Name =
		apply(singular, singular + "s", style)
	
	/**
	  * @param singular A singular version of this name
	  * @param expectedStyle Style in which this name is expected to be given
	  * @return Name based on the specified string
	  */
	def interpret(singular: String, expectedStyle: NamingConvention): Name =
		apply(singular, NamingConvention.of(singular, expectedStyle))
}

/**
  * Represents a class / property name
  * @author Mikko Hilpinen
  * @since 4.9.2021, v0.1
  */
case class Name(singular: String, plural: String, style: NamingConvention)
	extends SelfComparable[Name]
{
	// COMPUTED ------------------------------
	
	/**
	  * Different versions (singular & plural) of this name - doesn't contain duplicates
	  */
	lazy val variants = Vector(singular, plural).distinct
	
	/**
	  * @return This name in text format (lower case)
	  */
	def toText = to(Text.lower)
	
	/**
	  * @return Plural version of this name's text / documentation presentation
	  */
	def pluralText = Text.lower.convert(plural, style)
	/**
	  * @param naming Implicit naming convention
	  * @return A class name based on this name
	  */
	def className(implicit naming: NamingRules) = naming.className.convert(singular, style)
	/**
	  * @param naming Implicit naming convention
	  * @return A class name based on the plural version of this name
	  */
	def pluralClassName(implicit naming: NamingRules) = naming.className.convert(plural, style)
	/**
	  * @param naming Implicit naming convention
	  * @return A property name based on this name
	  */
	def propName(implicit naming: NamingRules) = naming.classProp.convert(singular, style)
	/**
	  * @param naming Implicit naming convention
	  * @return A property name based on this name applicable to contexts where multiple values are accepted
	  */
	def pluralPropName(implicit naming: NamingRules) = naming.classProp.convert(plural, style)
	/**
	  * @param naming Implicit naming convention
	  * @return An sql table name based on this name
	  */
	def tableName(implicit naming: NamingRules) = naming.table.convert(singular, style)
	/**
	  * @param naming Implicit naming convention
	  * @return An sql property name based on this name
	  */
	def columnName(implicit naming: NamingRules) = naming.column.convert(singular, style)
	/**
	  * @param naming Implicit naming convention
	  * @return A json property name based on this name
	  */
	def jsonPropName(implicit naming: NamingRules) = naming.jsonProp.convert(singular, style)
	
	
	// IMPLEMENTED  --------------------------
	
	override def toString = Text.lower.convert(singular, style)
	
	override def repr = this
	
	override def compareTo(o: Name) = singular.compareTo(o.singular)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param other Another name
	  * @return Whether these two names resemble each other (case- and naming style insensitive)
	  */
	def ~==(other: Name) = {
		val otherInMyStyle = other.to(style)
		if ((otherInMyStyle.singular ~== singular) || (otherInMyStyle.plural ~== plural))
			true
		else if (other.style == style)
			false
		else {
			val inOtherStyle = to(other.style)
			(inOtherStyle.singular ~== other.singular) || (inOtherStyle.plural ~== other.plural)
		}
	}
	
	/**
	  * @param string A string to append to the end of this name (applies to both singular and plural versions)
	  * @return An appended copy of this name
	  */
	def +(string: Name) = {
		val sameStyle = string.to(style)
		Name(style.combine(singular, sameStyle.singular), style.combine(plural, sameStyle.plural), style)
	}
	def +(string: String): Name = this + (string: Name)
	
	def +:(string: Name) = {
		val sameStyle = string.to(style)
		Name(style.combine(sameStyle.singular, singular), style.combine(sameStyle.plural, plural), style)
	}
	def +:(string: String): Name = string +: this
	
	/**
	  * @param style A naming convention
	  * @return A copy of this name in that naming convention
	  */
	def to(style: NamingConvention) = {
		if (this.style == style)
			this
		else
			Name(style.convert(singular, this.style), style.convert(plural, this.style), style)
	}
}
