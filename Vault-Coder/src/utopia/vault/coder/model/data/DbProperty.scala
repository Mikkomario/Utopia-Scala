package utopia.vault.coder.model.data

import utopia.coder.model.data.{Name, Named, NamingRules}
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.datatype.SqlTypeConversion
import utopia.coder.model.enumeration.NameContext.{ColumnName, DbModelPropName}

/**
  * A database-column matching property for a class. Some class properties may match to multiple database-column
  * properties.
  * @author Mikko Hilpinen
  * @since 18.7.2022, v1.5.1
  * @param parentName Name of the parent (class) property
  * @param conversion Conversion of this property to an sql property
  * @param overrides User-defined overrides for this property
  */
case class DbProperty(parentName: Name, conversion: SqlTypeConversion, overrides: DbPropertyOverrides) extends Named
{
	// COMPUTED ----------------------------
	
	/**
	  * @return Type of this property in the SQL document
	  */
	def sqlType = conversion.target
	
	/**
	  * @return Whether this property matches a database index
	  */
	def isIndexed = overrides.indexing.getOrElse(sqlType.indexByDefault)
	
	/**
	  * @return The default SQL value of this property
	  */
	def default = overrides.default.nonEmptyOrElse(sqlType.defaultValue)
	
	/**
	  * @param naming Implicit naming rules
	  * @return Column name to use for this property
	  */
	def columnName(implicit naming: NamingRules) =
		overrides.columnName.nonEmptyOrElse { (name + sqlType.columnNameSuffix).column }
	/**
	  * @param naming Implicit naming rules
	  * @return Name to use for this property in database model string literals
	  */
	def modelName(implicit naming: NamingRules) = naming(DbModelPropName).convert(columnName, naming(ColumnName))
	/**
	  * @param naming Implicit naming rules
	  * @return A json object property name matching this property
	  */
	def jsonPropName(implicit naming: NamingRules) = (name + sqlType.columnNameSuffix).jsonProp
	
	/**
	  * @param naming Implicit naming rules
	  * @return Property to value code - NB: This property is expected to be in the "intermediate" (db model) state
	  *         during this conversion
	  */
	def toValueCode(implicit naming: NamingRules) = conversion.intermediate.toValueCode(name.prop)
	
	
	// IMPLEMENTED  --------------------
	
	def name = overrides.name.getOrElse(parentName)
}