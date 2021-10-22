package utopia.vault.coder.model.data

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.enumeration.PropertyType
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.util.NamingUtils

object Property
{
	/**
	  * Creates a new property with automatic naming
	  * @param name Name of this property
	  * @param dataType Type of this property
	  * @param description Description of this property (Default = empty)
	  * @param useDescription Description on how this property is used (Default = empty)
	  * @param customDefault Default value passed for this property (empty if no default (default))
	  * @param customSqlDefault Default value passed for this property in the SQL table creation (empty if no default)
	  * @param customIndexing User-specified setting whether this property should be indexed.
	  *                       None if user didn't specify, which results in data type -based indexing (default)
	  * @return A new property
	  */
	def apply(name: Name, dataType: PropertyType, description: String = "", useDescription: String = "",
	          customDefault: String = "", customSqlDefault: String = "",
	          customIndexing: Option[Boolean] = None): Property =
		apply(name, NamingUtils.camelToUnderscore(name.singular), dataType, description, useDescription,
			customDefault, customSqlDefault, customIndexing)
}

/**
  * Classes define certain typed properties that are present in every instance
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param name Name of this property
  * @param columnName Name of the column linked with this property
  * @param dataType Type of this property
  * @param description Description of this property (may be empty)
  * @param useDescription Description on how this property is used (may be empty)
  * @param customDefault Default value passed for this property (empty if no default)
  * @param customSqlDefault Default value passed for this property in the SQL table creation (empty if no default)
  * @param customIndexing User-specified setting whether this property should be indexed.
  *                       None if user didn't specify, which results in data type -based indexing
  */
case class Property(name: Name, columnName: String, dataType: PropertyType, description: String,
                    useDescription: String, customDefault: String, customSqlDefault: String,
                    customIndexing: Option[Boolean])
{
	// COMPUTED ----------------------------
	
	/**
	  * @return Whether there exists an index based on this property
	  */
	def isIndexed = customIndexing.getOrElse(dataType.createsIndexByDefault)
	
	/**
	  * @return The default value assigned for this property. Empty if no default is provided.
	  */
	def default = customDefault.notEmpty match
	{
		case Some(default) => CodePiece(default)
		case None => dataType.baseDefault
	}
	/**
	  * @return The default value assigned for this property in the SQL document / table creation
	  */
	def sqlDefault = customSqlDefault.notEmpty.getOrElse(dataType.baseSqlDefault)
	
	/**
	  * @return Code for this property converted to a value. Expects ValueConversions to be imported.
	  */
	def toValueCode: CodePiece = dataType.toValueCode(name.singular)
	
	/**
	  * @return A nullable copy of this property
	  */
	def nullable = if (dataType.isNullable) this else copy(dataType = dataType.nullable)
}
