package utopia.vault.coder.model.data

import utopia.vault.coder.model.enumeration.BasicPropertyType.{IntNumber, LongNumber}
import utopia.vault.coder.model.enumeration.IntSize.Default
import utopia.vault.coder.model.enumeration.NamingConvention.CamelCase
import utopia.vault.coder.model.enumeration.PropertyType.{ClassReference, CreationTime, Deprecation, EnumValue, Expiration}

object Class
{
	/**
	  * Id / index name to use by default
	  */
	val defaultIdName = Name("id", "ids", CamelCase.lower)
	
	/**
	  * Creates a new class with automatic table-naming
	  * @param name Name of this class (in code)
	  * @param properties Properties in this class
	  * @param packageName Name of the package in which to wrap this class (default = empty)
	  * @param idName Name of this class' id property (default = id)
	  * @param description A description of this class (default = empty)
	  * @param author Author who wrote this class (may be empty)
	  * @param useLongId Whether to use long instead of int in the id property (default = false)
	  * @param writeGenericAccess Whether a generic access trait should be written for this class (includes combos)
	  *                           (default = false)
	  * @return A new class
	  */
	def apply(name: Name, properties: Vector[Property], packageName: String = "",
	          comboIndexColumnNames: Vector[Vector[String]] = Vector(), idName: Name = defaultIdName,
	          description: String = "", author: String = "", useLongId: Boolean = false,
	          writeGenericAccess: Boolean = false): Class =
		apply(name, None, idName, properties, packageName, comboIndexColumnNames, None,
			description, author, useLongId, writeGenericAccess)
	
	/**
	  * Creates a new class with automatic table-naming with description support
	  * @param name Name of this class (in code)
	  * @param properties Properties in this class
	  * @param packageName Name of the package in which to wrap this class (default = empty)
	  * @param idName Name of this class' id property (default = id)
	  * @param description A description of this class (default = empty)
	  * @param author Author who wrote this class (may be empty)
	  * @param descriptionLinkColumnName Name of the column that refers to this class from description links
	  *                                  (default = autogenerated)
	  * @param useLongId Whether to use long instead of int in the id property (default = false)
	  * @param writeGenericAccess Whether a generic access trait should be written for this class (includes combos)
	  *                           (default = false)
	  * @return A new class
	  */
	def described(name: Name, properties: Vector[Property], packageName: String = "",
	              comboIndexColumnNames: Vector[Vector[String]] = Vector(), idName: Name = defaultIdName,
	              description: String = "",
	              author: String = "", descriptionLinkColumnName: Option[Name] = None,
	              useLongId: Boolean = false, writeGenericAccess: Boolean = false): Class =
	{
		apply(name, None, idName, properties, packageName, comboIndexColumnNames,
			Some[Name](descriptionLinkColumnName.getOrElse { name + "id" }), description, author, useLongId,
			writeGenericAccess)
	}
}

/**
  * Represents a base class, from which multiple specific classes and interfaces are created
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param name Name of this class (in code)
  * @param customTableName Overridden name of this class' table (optional)
  * @param idName Name of this class' id (index) property
  * @param properties Properties in this class
  * @param packageName Name of the package in which to wrap this class (may be empty)
  * @param comboIndexColumnNames Combo-indices for this class. Each item (vector) represents a single combo-index.
  *                              The items (strings) in combo-indices represent column names.
  * @param descriptionLinkColumnName Name of the column that refers to this class from a description link
  *                                  (optional)
  * @param description A description of this class
  * @param useLongId Whether to use long instead of int in the id property
  * @param writeGenericAccess Whether a generic access trait should be written for this class (includes combos)
  */
// TODO: Rename description link column into description link (property)
case class Class(name: Name, customTableName: Option[String], idName: Name, properties: Vector[Property],
                 packageName: String, comboIndexColumnNames: Vector[Vector[String]],
                 descriptionLinkColumnName: Option[Name], description: String, author: String, useLongId: Boolean,
                 writeGenericAccess: Boolean)
{
	// ATTRIBUTES   ---------------------------------
	
	/**
	  * @return Class that links descriptions with instances of this class. None if not applicable to this class.
	  */
	lazy val descriptionLinkClass = descriptionLinkColumnName.map { linkColumnName =>
		val idName = Name("descriptionId", CamelCase.lower)
		val tableName = customTableName match {
			case Some(n) => n: Name
			case None => name
		}
		val props = Vector(
			Property(linkColumnName, ClassReference(tableName, idName, idType), s"Id of the described $name"),
			Property(idName, ClassReference("description"), "Id of the linked description")
		)
		Class(name + "description", props, "description",
			description = s"Links ${name.plural} with their descriptions", author = author)
	}
	
	
	// COMPUTED ------------------------------------
	
	/**
	  * @return Whether this class uses integer type ids
	  */
	def useIntId = !useLongId
	
	/**
	  * @return Type of the ids used in this class
	  */
	def idType =  if (useLongId) LongNumber else IntNumber(Default)
	
	/**
	  * @return Whether this class supports description linking
	  */
	def isDescribed = descriptionLinkColumnName.nonEmpty
	
	/**
	  * @return Whether this class records a row / instance creation time that is also an index
	  */
	def recordsIndexedCreationTime = properties.exists { p => p.dataType match {
		case CreationTime => p.isIndexed
		case _ => false
	} }
	
	/**
	  * @return Whether this class supports deprecation or expiration
	  */
	def isDeprecatable = properties.exists { _.dataType match {
		case Deprecation | Expiration => true
		case _ => false
	} }
	
	/**
	  * @return Whether this class refers to one or more enumerations in its properties
	  */
	def refersToEnumerations = properties.exists { _.dataType match {
		case _: EnumValue => true
		case _ => false
	} }
	
	/**
	  * @return The property in this class which contains instance creation time. None if no such property is present.
	  */
	def creationTimeProperty = properties.find { _.dataType match {
		case CreationTime => true
		case _ => false
	} }
	
	/**
	  * @return Property in this class which contains instance deprecation time. None if no such property is present.
	  */
	def deprecationProperty = properties.find { _.dataType match {
		case Deprecation => true
		case _ => false
	} }
	
	/**
	  * @return Property in this class which contains instance expiration time. None if no such property is present.
	  */
	def expirationProperty = properties.find { _.dataType match {
		case Expiration => true
		case _ => false
	} }
	
	/**
	  * @param naming Implicit naming rules
	  * @return Table name used for this class
	  */
	def tableName(implicit naming: NamingRules) = customTableName.getOrElse { name.tableName }
	/**
	  * @param naming Implicit naming rules
	  * @return Name used for this class' id property in database model string literals
	  */
	def idDatabasePropName(implicit naming: NamingRules) = naming.dbModelProp.convert(idName.columnName, naming.column)
}
