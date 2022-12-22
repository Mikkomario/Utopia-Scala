package utopia.vault.coder.model.data

import utopia.flow.generic.factory.FromValueFactory
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.{ApproxEquals, SelfComparable}
import utopia.flow.operator.EqualsExtensions._
import utopia.vault.coder.model.enumeration.NameContext.{ClassName, ClassPropName, ColumnName, Documentation, EnumName, EnumValueName, FileName, FunctionName, Header, JsonPropName, ObjectName, TableName}
import utopia.vault.coder.model.enumeration.{NameContext, NamingConvention}
import utopia.vault.coder.model.enumeration.NamingConvention.{CamelCase, Text}

import scala.collection.immutable.StringOps
import scala.language.implicitConversions

object Name extends FromValueFactory[Name]
{
	// IMPLICIT -----------------------------
	
	// Implicitly converts strings
	implicit def stringToName(name: String): Name = apply(name)
	
	
	// IMPLEMENTED  -------------------------
	
	override def default = Name("", "", CamelCase.lower)
	
	override def fromValue(value: Value) =
		value.model.filter { m => m.containsNonEmpty("singular") } match {
			case Some(model) =>
				val singular = model("singular").getString
				Some(apply(singular, model("plural").stringOr { pluralize(singular) },
					model("style").string.flatMap(NamingConvention.forName).getOrElse { NamingConvention.of(singular) }))
			case None => value.string.map(apply)
		}
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param singular A singular version of this name
	  * @return Name based on the specified string
	  */
	def apply(singular: String): Name = apply(singular, NamingConvention.of(singular))
	/**
	  * @param singular A singular version of this name
	  * @param context Implicit name context
	  * @param naming Implicit naming rules
	  * @return Name based on the specified string, interpreted according to the applicable context
	  */
	def contextual(singular: String)(implicit context: NameContext, naming: NamingRules) =
		interpret(singular, naming(context))
	
	/**
	  * @param singular A singular version of this name
	  * @param style Style in which this name is given
	  * @return Name based on the specified string
	  */
	def apply(singular: String, style: NamingConvention): Name =
		apply(singular, pluralize(singular), style)
	
	/**
	  * @param singular A singular version of this name
	  * @param expectedStyle Style in which this name is expected to be given
	  * @return Name based on the specified string
	  */
	def interpret(singular: String, expectedStyle: NamingConvention): Name =
		apply(singular, NamingConvention.of(singular, expectedStyle))
	
	private def pluralize(singular: String) = {
		// Case: Empty string => remains empty
		if (singular.isEmpty)
			""
		else
			singular.last.toLower match {
				// Case: Ends with an 's' => prepends with 'many' or replaces double 's' with 'sses'
				case 's' =>
					if (singular.length == 1 || (singular: StringOps)(singular.length - 2) == 's')
						singular + "ses"
					else
						singular + "es"
					/*else if (singular.startsWithIgnoreCase("many"))
						singular
					else {
						val style = NamingConvention.of(singular)
						style.combine(style.convert("many", Text.lower), singular)
					}*/
				case 'y' =>
					// Case: Ends with a y => replaces 'y' with 'ies'
					if (singular.last.isLower)
						singular.dropRight(1) + "ies"
					// Case: Ends with a Y (separate) => adds an 's'
					else if (singular.length == 1 || (singular: StringOps)(singular.length - 2).isLower)
						singular + 's'
					// Case: Ends with a Y (with other upper-case characters before that) => replaces 'Y' with 'IES'
					else
						singular.dropRight(1) + "IES"
				// Default => appends 's'
				case _ => singular + 's'
			}
	}
}

/**
  * Represents a class / property name
  * @author Mikko Hilpinen
  * @since 4.9.2021, v0.1
  */
case class Name(singular: String, plural: String, style: NamingConvention)
	extends SelfComparable[Name] with ApproxEquals[Name]
{
	// COMPUTED ------------------------------
	
	/**
	  * Different versions (singular & plural) of this name - doesn't contain duplicates
	  */
	lazy val variants = Vector(singular, plural).distinct
	
	/**
	  * @return This name in text format (lower case)
	  */
	@deprecated("Please use .doc or .pluralDoc instead", "v1.7")
	def toText = to(Text.lower)
	/**
	  * @return Plural version of this name's text / documentation presentation
	  */
	@deprecated("Please use .pluralDoc instead", "v1.7")
	def pluralText = Text.lower.convert(plural, style)
	
	/**
	  * @param naming Implicit naming rules
	  * @return Singular form of this name applicable for documentation context
	  */
	def doc(implicit naming: NamingRules) = apply(Documentation)
	/**
	  * @param naming Implicit naming rules
	  * @return Plural form of this name applicable for documentation context
	  */
	def pluralDoc(implicit naming: NamingRules) = pluralInContext(Documentation)
	/**
	  * @param naming Implicit naming rules
	  * @return A header name based on this name
	  */
	def header(implicit naming: NamingRules) = apply(Header)
	/**
	  * @param naming Implicit naming convention
	  * @return A class name based on this name
	  */
	def className(implicit naming: NamingRules) = apply(ClassName)
	/**
	  * @param naming Implicit naming convention
	  * @return An object name based on this name
	  */
	def objectName(implicit naming: NamingRules) = apply(ObjectName)
	/**
	  * @param naming Implicit naming convention
	  * @return A class name based on the plural version of this name
	  */
	def pluralClassName(implicit naming: NamingRules) = pluralInContext(ClassName)
	/**
	  * @param naming Implicit naming convention
	  * @return A property name based on this name
	  */
	def prop(implicit naming: NamingRules) = apply(ClassPropName)
	/**
	  * @param naming Implicit naming convention
	  * @return A property name based on this name applicable to contexts where multiple values are accepted
	  */
	def props(implicit naming: NamingRules) = pluralInContext(ClassPropName)
	/**
	  * @param naming Implicit naming rules
	  * @return This name as a singular function name
	  */
	def function(implicit naming: NamingRules) = apply(FunctionName)
	/**
	  * @param naming Implicit naming convention
	  * @return An sql table name based on this name
	  */
	def table(implicit naming: NamingRules) = apply(TableName)
	/**
	  * @param naming Implicit naming convention
	  * @return An sql property name based on this name
	  */
	def column(implicit naming: NamingRules) = apply(ColumnName)
	/**
	  * @param naming Implicit naming convention
	  * @return A json property name based on this name
	  */
	def jsonProp(implicit naming: NamingRules) = apply(JsonPropName)
	/**
	  * @param naming Implicit naming convention
	  * @return An enumeration (trait / object) name based on this name
	  */
	def enumName(implicit naming: NamingRules) = apply(EnumName)
	/**
	  * @param naming Implicit naming convention
	  * @return An enumeration value (object) name based on this name
	  */
	def enumValue(implicit naming: NamingRules) = apply(EnumValueName)
	/**
	  * @param naming Implicit naming rules
	  * @return A file name based on this name
	  */
	def fileName(implicit naming: NamingRules) = apply(FileName)
	
	
	// IMPLEMENTED  --------------------------
	
	override def toString = singularIn(Text.lower)
	def toString(implicit naming: NamingRules) = doc
	def toString(implicit context: NameContext, naming: NamingRules) = contextualSingular
	
	override def self = this
	
	override def compareTo(o: Name) = singular.compareTo(o.singular)
	
	/**
	  * @param other Another name
	  * @return Whether these two names resemble each other (case- and naming style insensitive)
	  */
	override def ~==(other: Name) = {
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
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param style Targeted naming convention
	  * @return Singular form of this name in the specified naming convention
	  */
	def singularIn(style: NamingConvention) = style.convert(singular, this.style)
	/**
	  * @param style Targeted naming convention
	  * @return Plural form of this name in the specified naming convention
	  */
	def pluralIn(style: NamingConvention) = style.convert(plural, this.style)
	/**
	  * @param style Targeted naming convention
	  * @return Singular form of this name in the specified naming convention
	  */
	def apply(style: NamingConvention) = singularIn(style)
	
	/**
	  * @param context Applicable name context
	  * @param naming Implicit naming rules
	  * @return Singular form of this name in appropriate style for the specified context
	  */
	def singularInContext(context: NameContext)(implicit naming: NamingRules) = singularIn(naming(context))
	/**
	  * @param context Applicable name context
	  * @param naming Implicit naming rules
	  * @return Plural form of this name in appropriate style for the specified context
	  */
	def pluralInContext(context: NameContext)(implicit naming: NamingRules) = pluralIn(naming(context))
	/**
	  * @param context Applicable name context
	  * @param naming Implicit naming rules
	  * @return Singular form of this name in appropriate style for the specified context
	  */
	def apply(context: NameContext)(implicit naming: NamingRules) = singularInContext(context)
	
	/**
	  * @param context Implicit name context
	  * @param naming Implicit naming rules
	  * @return Singular form of this name in appropriate style for the applicable context
	  */
	def contextualSingular(implicit context: NameContext, naming: NamingRules) = singularInContext(context)
	/**
	  * @param context Implicit name context
	  * @param naming Implicit naming rules
	  * @return Plural form of this name in appropriate style for the applicable context
	  */
	def contextualPlural(implicit context: NameContext, naming: NamingRules) = pluralInContext(context)
	
	/**
	  * @param name A string representing another name
	  * @return Whether these two names resemble each other
	  */
	def ~==(name: String): Boolean = {
		lazy val sameStyleName = style.convert(name, NamingConvention.of(name, style))
		variants.exists { v => (v ~== name) || (v ~== sameStyleName) }
	}
	
	/**
	  * @param string A string to append to the end of this name (applies to both singular and plural versions)
	  * @return An appended copy of this name
	  */
	def +(string: Name) = {
		val sameStyle = string.to(style)
		// The leftmost name is no longer pluralized in the combination
		// (except when the rightmost part is indistinguishable)
		val pluralFirstPart = if (sameStyle.singular == sameStyle.plural) plural else singular
		Name(style.combine(singular, sameStyle.singular), style.combine(pluralFirstPart, sameStyle.plural), style)
	}
	def +(string: String): Name = if (string.isEmpty) this else this + (string: Name)
	
	def +:(string: Name) = string.to(style) + this
	def +:(string: String): Name = if (string.isEmpty) this else (string: Name) +: this
	def ++(strings: IterableOnce[String]): Name = strings.iterator.foldLeft(this) { _ + _ }
	
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
	/**
	  * @param context Name context
	  * @param naming Implicit naming rules to apply
	  * @return A copy of this name in style appropriate for the specified context
	  */
	def inContext(context: NameContext)(implicit naming: NamingRules) = to(naming(context))
	/**
	  * @param context Implicit name context
	  * @param naming Implicit naming rules to apply
	  * @return A copy of this name in style appropriate for the applicable context
	  */
	def contextual(implicit context: NameContext, naming: NamingRules) = inContext(context)
}
