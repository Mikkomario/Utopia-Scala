package utopia.vault.coder.controller.writer.database

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Name, NamingRules, ProjectSetup, Property}
import utopia.vault.coder.model.enumeration.NamingConvention.CamelCase
import utopia.vault.coder.model.scala
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue}
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration, PropertyDeclaration}
import utopia.vault.coder.model.scala.{DeclarationDate, Extension, Parameter, Reference, ScalaType}

import _root_.scala.io.Codec

/**
  * Used for writing database model scala files
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object DbModelWriter
{
	// ATTRIBUTES   -------------------------------------
	
	/**
	  * Suffix added to class name in order to make it a database model class name
	  */
	val classNameSuffix = Name("Model", "Models", CamelCase.capitalized)
	/**
	  * Suffix added to class property names in order to make them property name attributes
	  */
	val attNameSuffix = Name("AttName", "AttNames", CamelCase.capitalized)
	/**
	  * Suffix added to class property names in order to make them column attributes
	  */
	val columnNameSuffix = Name("Column", "Columns", CamelCase.capitalized)
	
	private val withMethodPrefix = Name("with", "with", CamelCase.lower)
	
	
	// OTHER    -----------------------------------------
	
	/**
	  * @param propName a property name
	  * @param naming Naming rules to apply
	  * @return Name of the property that refers to this property's db property name
	  */
	def attNameFrom(propName: Name)(implicit naming: NamingRules) = (propName + attNameSuffix).propName
	/**
	  * @param prop A property
	  * @param naming Naming rules to apply
	  * @return Name of the property that refers to this property's db property name
	  */
	def attNameFrom(prop: Property)(implicit naming: NamingRules): String = attNameFrom(prop.name)
	/**
	  * @param propName A property name
	  * @param naming Naming rules to apply
	  * @return Name of the property that refers to this property's column
	  */
	def columnNameFrom(propName: Name)(implicit naming: NamingRules) = (propName + columnNameSuffix).propName
	/**
	  * @param prop A property
	  * @param naming Naming rules to apply
	  * @return Name of the property that refers to this property's column
	  */
	def columnNameFrom(prop: Property)(implicit naming: NamingRules): String = columnNameFrom(prop.name)
	
	/**
	  * Generates the DB model class and the associated companion object
	  * @param classToWrite The base class
	  * @param modelRef     Reference to the stored model class
	  * @param dataRef      Reference to the data class
	  * @param factoryRef   Reference to the factory class
	  * @param codec        Implicit codec used when writing the file
	  * @param setup        Target project -specific setup (implicit)
	  * @return Reference to the generated class. Failure if writing failed.
	  */
	def apply(classToWrite: Class, modelRef: Reference, dataRef: Reference, factoryRef: Reference)
	         (implicit codec: Codec, setup: ProjectSetup, naming: NamingRules) =
	{
		val parentPackage = setup.dbModelPackage / classToWrite.packageName
		val className = (classToWrite.name + classNameSuffix).className
		val deprecation = DeprecationStyle.of(classToWrite)
		
		// The generated file contains the model class and the associated companion object
		File(parentPackage,
			ObjectDeclaration(className,
				factoryExtensionsFor(className, modelRef, dataRef, deprecation),
				// Contains xAttName and xColumn for each property, as well as factory and table -properties
				properties = classToWrite.properties.flatMap { prop =>
					val attName = attNameFrom(prop)
					Vector(
						ImmutableValue(attName,
							description = s"Name of the property that contains ${ classToWrite.name } ${ prop.name }")(
							prop.dbModelPropName.quoted),
						ComputedProperty(columnNameFrom(prop),
							description = s"Column that contains ${ classToWrite.name } ${ prop.name }")(
							s"table($attName)")
					)
				} ++ Vector(
					ComputedProperty("factory", Set(factoryRef),
						description = "The factory object used by this model type")(factoryRef.target),
					ComputedProperty("table", isOverridden = true)("factory.table")
				) ++ deprecation.iterator.flatMap { _.properties },
				// Implements .apply(...) and .complete(id, data)
				// Also includes withX(...) methods for each property
				methods = Set(
					MethodDeclaration("apply", isOverridden = true)(Parameter("data", dataRef))(
						s"apply(${
							("None" +: classToWrite.properties.map { prop =>
								if (prop.dataType.isNullable)
									s"data.${ prop.name.propName }"
								else
									s"Some(data.${ prop.name.propName })"
							}).mkString(", ")
						})"),
					MethodDeclaration("complete", Set(modelRef), isOverridden = true)(
						Vector(Parameter("id", Reference.value), Parameter("data", dataRef)))(
						s"${ modelRef.target }(id.get${ if (classToWrite.useLongId) "Long" else "Int" }, data)"),
					MethodDeclaration("withId", returnDescription = "A model with that id")(
						Parameter("id", classToWrite.idType.toScala, description = s"A ${ classToWrite.name } id"))(
						"apply(Some(id))")
				) ++ classToWrite.properties.map { prop =>
					val paramName = prop.name.propName
					MethodDeclaration(withMethodNameFor(prop),
						returnDescription = s"A model containing only the specified ${ prop.name }")(
						scala.Parameter(paramName, prop.dataType.notNull.toScala, description = prop.description))(
						s"apply($paramName = Some($paramName))")
				} ++ deprecation.iterator.flatMap { _.methods },
				description = s"Used for constructing $className instances and for inserting ${
					classToWrite.name.pluralText
				} to the database", author = classToWrite.author, since = DeclarationDate.versionedToday
			),
			ClassDeclaration(className,
				// Accepts a copy of all properties where each is wrapped in option (unless already an option)
				Parameter("id", classToWrite.idType.nullable.toScala, "None",
					description = s"${ classToWrite.name } database id") +:
					classToWrite.properties.map { prop =>
						val inputType = prop.dataType.nullable
						val defaultValue = inputType.baseDefault.notEmpty.getOrElse(CodePiece("None"))
						Parameter(prop.name.propName, inputType.toScala, defaultValue, description = prop.description)
					},
				// Extends StorableWithFactory[A]
				Vector(Reference.storableWithFactory(modelRef)),
				// Implements the required properties: factory & valueProperties
				properties = Vector(
					ComputedProperty("factory", isOverridden = true)(s"$className.factory"),
					valuePropertiesPropertyFor(classToWrite, className)
				),
				// adds withX(...) -methods for convenience
				methods = classToWrite.properties.map { prop =>
					val paramName = prop.name.propName
					MethodDeclaration(withMethodNameFor(prop),
						returnDescription = s"A new copy of this model with the specified ${ prop.name }")(
						Parameter(paramName, prop.dataType.notNull.toScala,
							description = s"A new ${ prop.name }"))(
						s"copy($paramName = Some($paramName))")
				}.toSet,
				description = s"Used for interacting with ${ classToWrite.name.plural } in the database",
				author = classToWrite.author, since = DeclarationDate.versionedToday, isCaseClass = true)
		).write()
	}
	
	private def factoryExtensionsFor(className: String, modelRef: Reference, dataRef: Reference,
	                                 deprecation: Option[DeprecationStyle]): Vector[Extension] =
	{
		// The class itself doesn't need to be imported (same file)
		val classType = ScalaType.basic(className)
		// All factories extend the DataInserter trait
		val dataInserter = Reference.dataInserter(classType, modelRef, dataRef)
		// They may also extend a deprecation-related trait
		deprecation match {
			case Some(deprecation) => Vector(dataInserter, deprecation.extensionFor(classType))
			case None => Vector(dataInserter)
		}
	}
	
	private def valuePropertiesPropertyFor(classToWrite: Class, className: String)
	                                      (implicit naming: NamingRules) =
	{
		val quotedId = classToWrite.idDatabasePropName.quoted
		if (classToWrite.properties.isEmpty)
			ComputedProperty("valueProperties", Set(Reference.valueConversions), isOverridden = true)(
				s"Vector($quotedId -> id)"
			)
		else {
			val propsPart = classToWrite.properties
				.map { prop => prop.nullable.toValueCode.withPrefix(s"${ attNameFrom(prop) } -> ") }
				.reduceLeft { _.append(_, ", ") }
			ComputedProperty("valueProperties", propsPart.references + Reference.valueConversions, isOverridden = true)(
				s"import $className._",
				s"Vector($quotedId -> id, $propsPart)"
			)
		}
	}
	
	private def withMethodNameFor(prop: Property)(implicit naming: NamingRules) =
		(withMethodPrefix + prop.name).propName
	
	
	// NESTED   --------------------------------------
	
	private sealed trait DeprecationStyle
	{
		def extensionFor(dbModelClass: ScalaType): Extension
		
		def properties(implicit naming: NamingRules): Vector[PropertyDeclaration]
		
		def methods: Set[MethodDeclaration]
	}
	
	private object DeprecationStyle
	{
		def of(c: Class) =
			c.deprecationProperty
				.map { deprecationProp =>
					c.expirationProperty match {
						case Some(expirationProp) =>
							CombinedDeprecation(expirationProp.name, deprecationProp.name)
						case None => NullDeprecates(deprecationProp)
					}
				}
				.orElse { c.expirationProperty.map { prop => Expires(prop) } }
	}
	
	private case class NullDeprecates(prop: Property) extends DeprecationStyle
	{
		// ATTRIBUTES   -----------------------------
		
		private val camelPropName = prop.name.to(CamelCase.lower).singular
		// Whether this deprecation matches the expected default
		val isDefault = camelPropName == "deprecatedAfter"
		
		
		// IMPLEMENTED  -----------------------------
		
		override def extensionFor(dbModelClass: ScalaType) =
			(if (isDefault) Reference.deprecatableAfter else Reference.nullDeprecatable)(dbModelClass)
		
		override def properties(implicit naming: NamingRules) =
			if (isDefault) Vector() else Vector(
				ImmutableValue("deprecationAttName", isOverridden = true)(prop.dbModelPropName.quoted))
		// withDeprecatedAfter(...) must be provided separately for custom property names
		override def methods = if (isDefault) Set() else Set(
			MethodDeclaration("withDeprecatedAfter", isOverridden = true)(
				Parameter("deprecationTime", Reference.instant))(s"with${ camelPropName.capitalize }(deprecationTime)")
		)
	}
	
	private case class Expires(prop: Property) extends DeprecationStyle
	{
		override def extensionFor(dbModelClass: ScalaType) = Reference.expiring
		
		override def properties(implicit naming: NamingRules) =
			Vector(ImmutableValue("deprecationAttName", isOverridden = true)(prop.dbModelPropName.quoted))
		override def methods = Set()
	}
	
	private case class CombinedDeprecation(expirationPropName: Name, deprecationPropName: Name)
		extends DeprecationStyle
	{
		override def extensionFor(dbModelClass: ScalaType) = Reference.deprecatable
		
		override def properties(implicit naming: NamingRules) = Vector(
			ComputedProperty("nonDeprecatedCondition", Set(Reference.valueConversions, Reference.sqlExtensions,
				Reference.now), isOverridden = true)(
				s"${columnNameFrom(deprecationPropName)}.isNull && ${columnNameFrom(expirationPropName)} > Now")
		)
		override def methods = Set()
	}
}
