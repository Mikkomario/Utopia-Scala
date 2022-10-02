package utopia.vault.coder.model.enumeration

import utopia.flow.collection.OptionsIterator
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.operator.ApproximatelyEquatable
import utopia.flow.util.ScopeUsable
import utopia.vault.coder.model.data.{Name, NamingRules}
import utopia.vault.coder.model.enumeration.NamingConvention.{CamelCase, Hyphenated, Text, UnderScore}

/**
  * A common trait for all name context values. These determine how a name should be displayed.
  * @author Mikko Hilpinen
  * @since 20.8.2022, v1.6
  */
sealed trait NameContext extends ScopeUsable[NameContext] with ApproximatelyEquatable[NameContext]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The default naming convention used in this context
	  */
	def defaultNaming: NamingConvention
	
	/**
	  * @return The context that is applicable as a general placeholder of this context
	  */
	def parent: Option[NameContext]
	
	/**
	  * @return Names of the json properties used to represent this context. In underscore format.
	  *         Parent type keys should not be listed. The keys should be ordered from most precise to least precise.
	  */
	def jsonProps: Seq[String]
	
	
	// COMPUTED ----------------------
	
	/**
	  * @param rules Implicit naming rules
	  * @return A naming convention appropriate in this context
	  */
	def style(implicit rules: NamingRules) = rules(this)
	
	/**
	  * @return An iterator that returns the parents of this context, going upwards (towards more generic)
	  */
	def parentsIterator = OptionsIterator.iterate(parent) { _.parent }
	
	
	// IMPLEMENTED  -----------------
	
	override def repr = this
	
	override def ~==(other: NameContext) =
		(this == other) || parentsIterator.contains(other) || other.parentsIterator.contains(this)
	
	
	// OTHER    --------------------
	
	/**
	  * Finds a name of this type from a model (includes plural name form)
	  * @param model Model from which a name is searched from
	  * @param disableGeneric Whether more generic type search should be disabled
	  * @param naming Implicit naming rules
	  * @return A name read from the specified model
	  */
	def from(model: Model[Property], disableGeneric: Boolean = false)(implicit naming: NamingRules): Option[Name] = {
		// Searches with the json keys of this style first
		_from(model, disableGeneric = true)
			// Uses the generic key "name" if no default keys are specified (unless disabled)
			.orElse {
				if (disableGeneric)
					None
				else
					model("name").string.map { n => Name.interpret(n, style) }
						// Otherwise may use more generic name keys, unless disabled
						.orElse { parent.flatMap { _.from(model) } }
			}
			.map { name =>
				val defaultPluralKeys = jsonProps.map { "plural_" + _ } ++ jsonProps.map { _ + "_plural" }
				val allPluralKeys = {
					if (disableGeneric)
						defaultPluralKeys
					else
						defaultPluralKeys ++ Vector("name_plural", "plural_name", "plural")
				}
				// Checks for a plural form, also
				model(allPluralKeys).string match {
					// Case: Plural form defined => converts it to the same style and adds it to the name
					case Some(plural) => name.copy(plural = name.style.convert(plural, style))
					// Case: Plural form not defined => uses the auto-generated plural form
					case None => name
				}
			}
	}
	
	private def _from(model: Model[Property], disableGeneric: Boolean = false)(implicit naming: NamingRules): Option[Name] = {
		// Looks for some specified property
		val default = model(jsonProps).string.map { name => Name.interpret(name, style) }
		// May use recursion for more generic keys (if not disabled)
		if (disableGeneric) default else default.orElse { parent.flatMap { _._from(model) } }
	}
}

object NameContext
{
	// ATTRIBUTES   ------------------
	
	/**
	  * All name context values
	  */
	val values = Vector[NameContext](EnumValueName, EnumName, JsonPropName, FunctionName, ClassPropName, ObjectName,
		ClassName, ColumnName, TableName, DatabaseName, Sql, Header, Documentation, FileName)
	
	
	// NESTED   ----------------------
	
	/**
	  * Name context for all SQL syntax
	  */
	case object Sql extends NameContext
	{
		override def defaultNaming = UnderScore
		override def parent = None
		override lazy val jsonProps = Vector("sql_name", "sql")
	}
	/**
	  * Name context for database names in SQL
	  */
	case object DatabaseName extends NameContext
	{
		override def defaultNaming = UnderScore
		override def parent = Some(Sql)
		override lazy val jsonProps = Vector("database_name", "db_name", "database", "db")
	}
	/**
	  * Name context for table names in SQL
	  */
	case object TableName extends NameContext
	{
		override def defaultNaming = UnderScore
		override def parent = Some(Sql)
		override lazy val jsonProps = Vector("table_name", "table")
	}
	/**
	  * Name context for column names in SQL
	  */
	case object ColumnName extends NameContext
	{
		override def defaultNaming = UnderScore
		override def parent = Some(Sql)
		override lazy val jsonProps = Vector("column_name", "col_name", "column", "col")
	}
	/**
	  * Name context for class names in Scala
	  */
	case object ClassName extends NameContext
	{
		override def defaultNaming = CamelCase.capitalized
		override def parent = None
		override lazy val jsonProps = Vector("class_name", "class", "instance_name", "instance")
	}
	/**
	  * Name context for object names in Scala
	  */
	case object ObjectName extends NameContext
	{
		override def defaultNaming = CamelCase.capitalized
		override def parent = Some(ClassName)
		override lazy val jsonProps = Vector("object_name", "obj_name", "object", "obj")
	}
	/**
	  * Name context for class / object / instance properties (and other class elements) in scala
	  */
	case object ClassPropName extends NameContext
	{
		override def defaultNaming = CamelCase.lower
		override def parent = None
		override lazy val jsonProps = Vector("class_property_name", "class_prop_name", "property_name", "prop_name",
			"property", "prop", "val")
	}
	/**
	  * Name context for functions / methods in Scala
	  */
	case object FunctionName extends NameContext
	{
		override def defaultNaming = CamelCase.lower
		override def parent = Some(ClassPropName)
		override lazy val jsonProps = Vector("function_name", "function", "method_name", "method", "def")
	}
	/**
	  * Name context for database models (i.e. intermediate models used in Vault operations)
	  */
	case object DbModelPropName extends NameContext
	{
		override def defaultNaming = CamelCase.lower
		override def parent = Some(ClassPropName)
		override lazy val jsonProps = Vector("database_model_property_name", "db_model_property_name", "db_model_prop_name",
			"database_model_property", "db_model_property", "db_model_prop", "database_property_name",
			"db_property_name", "db_prop_name", "database_property", "db_property", "db_prop")
	}
	/**
	  * Name context for json properties (/ json in general)
	  */
	case object JsonPropName extends NameContext
	{
		override def defaultNaming = CamelCase.lower
		override def parent = Some(ClassPropName)
		override lazy val jsonProps = Vector("json_property_name", "json_property", "json_prop_name", "json_prop", "json")
	}
	/**
	  * Name context for enumerations
	  */
	case object EnumName extends NameContext
	{
		override def defaultNaming = CamelCase.capitalized
		override def parent = Some(ClassName)
		override lazy val jsonProps = Vector("enumeration_name", "enum_name", "enumeration", "enum")
	}
	/**
	  * Name context for enumeration values
	  */
	case object EnumValueName extends NameContext
	{
		override def defaultNaming = CamelCase.capitalized
		override def parent = Some(EnumName)
		override lazy val jsonProps = Vector("enumeration_value_name", "enumeration_value", "enum_value_name", "enum_value",
			"enum_val_name", "enum_val")
	}
	/**
	  * Name context for documentation and text descriptions
	  */
	case object Documentation extends NameContext
	{
		override def defaultNaming = Text.lower
		override def parent = None
		override lazy val jsonProps = Vector("documentation", "doc", "text")
	}
	/**
	  * Name context for documentation headers
	  */
	case object Header extends NameContext
	{
		override def defaultNaming = Text.allCapitalized
		override def parent = Some(Documentation)
		override lazy val jsonProps =
			Vector("header_name", "heading_name", "title_name", "header", "heading", "head", "title")
	}
	/**
	  * Name context for file names
	  */
	case object FileName extends NameContext
	{
		override def defaultNaming = Hyphenated
		override def parent = None
		override def jsonProps = Vector("file_name", "file", "path_name", "path")
	}
}
