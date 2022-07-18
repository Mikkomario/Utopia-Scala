package utopia.vault.coder.model.data

import utopia.flow.util.CollectionExtensions._
import utopia.vault.coder.model.datatype.PropertyType.ClassReference
import utopia.vault.coder.model.datatype.{PropertyType, SingleColumnPropertyType}
import utopia.vault.coder.model.scala.code.CodePiece

object Property
{
	/**
	  * Creates a new property with automatic naming
	  * @param name Name of this property
	  * @param dataType Type of this property
	  * @param customDefaultValue Default value passed for this property, as code (empty if no default (default))
	  * @param description Description of this property (Default = empty)
	  * @return A new property
	  */
	def singleColumn(name: Name, dataType: SingleColumnPropertyType, customDefaultValue: CodePiece = CodePiece.empty,
	                 dbPropertyOverrides: DbPropertyOverrides = DbPropertyOverrides.empty,
	                 description: String = ""): Property =
		apply(name, dataType, customDefaultValue, Vector(dbPropertyOverrides), description)
}

/**
  * Classes define certain typed properties that are present in every instance
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param name Name of this property
  * @param dataType Type of this property
  * @param customDefaultValue Default value (code) that overrides the datatype-specified default.
  *                           Empty if no override should be made. Default = empty.
  * @param dbPropertyOverrides User-defined overrides applied to the database-properties matching this class-property.
  *                            Default = empty = no overrides.
  * @param description Description of this property (may be empty). Default = empty = no description.
  */
case class Property(name: Name, dataType: PropertyType, customDefaultValue: CodePiece = CodePiece.empty,
                    dbPropertyOverrides: Vector[DbPropertyOverrides] = Vector(), description: String = "")
	extends Named
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * Database-matching properties associated with this class-property
	  */
	lazy val dbProperties =  {
		val conversions = dataType.sqlConversions
		val conversionCount = conversions.size
		// Case: Single-column property => generates a single dbProperty
		if (conversionCount == 1)
			Vector(DbProperty(name, conversions.head,
				dbPropertyOverrides.headOption.getOrElse(DbPropertyOverrides.empty)))
		// Case: Multi-column property => generates multiple dbProperties with distinct names
		else
			conversions.indices.map { i =>
				// Makes sure custom names are defined
				val overrides = dbPropertyOverrides.getOption(i) match {
					// Case: Custom overrides exist => makes sure they include a custom name
					case Some(o) => if (o.name.isDefined) o else o.copy(name = Some(name + (i + 1).toString))
					// Case: No custom override exists => creates one that overrides the default property name
					case None => DbPropertyOverrides(Some(name + (i + 1).toString))
				}
				DbProperty(name, conversions(i), overrides)
			}.toVector
	}
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return Whether this property matches a single database-column
	  */
	def isSingleColumn = dataType.isSingleColumn
	/**
	  * @return Whether this property matches multiple database-columns
	  */
	def isMultiColumn = dataType.isMultiColumn
	
	/**
	  * @return Whether there exists an index based on this property
	  */
	def isIndexed = dbProperties.exists { _.isIndexed }
	
	/**
	  * @return Some(DbProperty) if this property only matches to a single database-property. None otherwise.
	  */
	def onlyDbVariant = if (isSingleColumn) dbProperties.headOption else None
	/**
	  * @return Left(DbProperty) if this property only has a single db-variant. Right(Vector(DbProperty))
	  *         if this property has multiple db-variants.
	  */
	def oneOrManyDbVariants =
		if (isSingleColumn) Left(dbProperties.head) else Right(dbProperties)
	
	/**
	  * @return The default value assigned for this property. Empty if no default is provided.
	  */
	def defaultValue = customDefaultValue.notEmpty.getOrElse(dataType.defaultValue)
	
	/**
	  * @return Name of the table referenced by this property. None if no references are made.
	  */
	def referencedTableName = dataType match {
		case ClassReference(tableName, _, _) => Some(tableName)
		case _ => None
	}
	
	/**
	  * @return A concrete (ie. non-optional) version of this property, if possible.
	  *         This if already a concrete property.
	  *         Please note that custom default values may be erased during this conversion.
	  */
	def concrete = {
		if (dataType.isConcrete)
			this
		else
			copy(dataType = dataType.concrete, customDefaultValue = CodePiece.empty)
	}
	
	/**
	  * @param naming Implicit naming rules
	  * @return Name of this property in json models
	  */
	// TODO: If json value conversion changes at some point (to support models, for example),
	//  this logic should be revisited
	def jsonPropName(implicit naming: NamingRules) =
		onlyDbVariant match {
			case Some(variant) => variant.jsonPropName
			case None => name.jsonPropName
		}
	
	/**
	  * @return Code for this property converted to a value. Expects ValueConversions to be imported.
	  */
	def toValueCode(implicit naming: NamingRules): CodePiece = dataType.toValueCode(name.propName)
	
	/*
	/**
	  * @return An optional copy of this property
	  */
	def optional = if (dataType.isOptional) this else copy(dataType = dataType.optional)
	/**
	  * @return A concrete (non-optional) copy of this property
	  */
	def concrete = if (dataType.isConcrete) this else copy(dataType = dataType.concrete)
	*/
}
