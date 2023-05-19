package utopia.vault.coder.controller.writer.database

import utopia.coder.model.data
import utopia.coder.model.data.{Name, Named, NamingRules}
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, DbProperty, VaultProjectSetup, Property}
import utopia.coder.model.enumeration.NamingConvention.CamelCase
import utopia.coder.model.scala
import utopia.coder.model.scala.Visibility.Protected
import utopia.coder.model.scala.datatype.{Extension, Reference, ScalaType}
import utopia.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue}
import utopia.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration, PropertyDeclaration}
import utopia.coder.model.scala.{DeclarationDate, Parameter}

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
	val classNameSuffix = data.Name("Model", "Models", CamelCase.capitalized)
	/**
	  * Suffix added to class property names in order to make them property name attributes
	  */
	val attNameSuffix = data.Name("AttName", "AttNames", CamelCase.capitalized)
	/**
	  * Suffix added to class property names in order to make them column attributes
	  */
	val columnNameSuffix = data.Name("Column", "Columns", CamelCase.capitalized)
	
	private val withMethodPrefix = data.Name("with", "with", CamelCase.lower)
	
	
	// OTHER    -----------------------------------------
	
	/**
	  * @param propName a property name
	  * @param naming Naming rules to apply
	  * @return Name of the property that refers to this property's db property name
	  */
	def attNameFrom(propName: Name)(implicit naming: NamingRules) = (propName + attNameSuffix).prop
	/**
	  * @param prop A property
	  * @param naming Naming rules to apply
	  * @return Name of the property that refers to this property's db property name
	  */
	def attNameFrom(prop: DbProperty)(implicit naming: NamingRules): String = attNameFrom(prop.name)
	/**
	  * @param propName A property name
	  * @param naming Naming rules to apply
	  * @return Name of the property that refers to this property's column
	  */
	def columnNameFrom(propName: Name)(implicit naming: NamingRules) = (propName + columnNameSuffix).prop
	/**
	  * @param prop A property
	  * @param naming Naming rules to apply
	  * @return Name of the property that refers to this property's column
	  */
	def columnNameFrom(prop: DbProperty)(implicit naming: NamingRules): String = columnNameFrom(prop.name)
	
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
	         (implicit codec: Codec, setup: VaultProjectSetup, naming: NamingRules) =
	{
		val parentPackage = setup.dbModelPackage / classToWrite.packageName
		val className = (classToWrite.name + classNameSuffix).className
		val deprecation = DeprecationStyle.of(classToWrite)
		
		val optionalIdType = classToWrite.idType.optional
		
		// Converts each property to the "intermediate" state
		val applyParametersCode = ("None" +: classToWrite.properties.flatMap { prop =>
			val propAccessCode = s"data.${prop.name.prop}"
			prop.dataType.sqlConversions
				.map { conversion => conversion.midConversion(propAccessCode) }
		}).mkString(", ")
		
		// The generated file contains the model class and the associated companion object
		File(parentPackage,
			ObjectDeclaration(className,
				factoryExtensionsFor(className, modelRef, dataRef, deprecation),
				// Contains xAttName and xColumn for each property, as well as factory and table -properties
				properties = classToWrite.dbProperties.flatMap { prop =>
					val attName = attNameFrom(prop)
					Vector(
						ImmutableValue(attName,
							description = s"Name of the property that contains ${ classToWrite.name.doc } ${ prop.name.doc }")(
							prop.modelName.quoted),
						ComputedProperty(columnNameFrom(prop),
							description = s"Column that contains ${ classToWrite.name.doc } ${ prop.name.doc }")(
							s"table($attName)")
					)
				}.toVector ++ Vector(
					ComputedProperty("factory", Set(factoryRef),
						description = "The factory object used by this model type")(factoryRef.target),
					ComputedProperty("table", isOverridden = true)("factory.table")
				) ++ deprecation.iterator.flatMap { _.properties },
				// Implements .apply(...) and .complete(id, data)
				methods = Set(
					MethodDeclaration("apply", isOverridden = true)(Parameter("data", dataRef))(
						s"apply($applyParametersCode)"),
					MethodDeclaration("complete", Set(modelRef), visibility = Protected, isOverridden = true)(
						Vector(Parameter("id", Reference.value), Parameter("data", dataRef)))(
						s"${ modelRef.target }(id.get${ if (classToWrite.useLongId) "Long" else "Int" }, data)"),
					MethodDeclaration("withId", returnDescription = "A model with that id")(
						Parameter("id", classToWrite.idType.toScala, description = s"A ${ classToWrite.name.doc } id"))(
						"apply(Some(id))")
					// Also includes withX(...) methods for each property
				) ++ classToWrite.properties.flatMap { withPropertyMethods(_) } ++
					deprecation.iterator.flatMap { _.methods },
				description = s"Used for constructing $className instances and for inserting ${
					classToWrite.name.pluralDoc
				} to the database", author = classToWrite.author, since = DeclarationDate.versionedToday
			),
			ClassDeclaration(className,
				// Accepts a copy of all properties where each appears in the "intermediate "(db property) state
				constructionParams = Parameter("id", optionalIdType.toScala, optionalIdType.emptyValue,
					description = s"${ classToWrite.name.doc } database id") +:
					classToWrite.dbProperties.map { prop =>
						val inputType = prop.conversion.intermediate
						val defaultValue = inputType.emptyValue
						// TODO: Parameter descriptions are missing
						Parameter(prop.name.prop, inputType.scalaType, defaultValue)
					}.toVector,
				// Extends StorableWithFactory[A]
				extensions = Vector(Reference.storableWithFactory(modelRef)),
				// Implements the required properties: factory & valueProperties
				properties = Vector(
					ComputedProperty("factory", isOverridden = true)(s"$className.factory"),
					valuePropertiesPropertyFor(classToWrite, className)
				),
				// adds withX(...) -methods for convenience
				methods = classToWrite.properties.flatMap { withPropertyMethods(_, "copy",
					"A new copy of this model with the specified ") }.toSet,
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
			val propsPart = classToWrite.dbProperties
				.map { prop => prop.toValueCode.withPrefix(s"${ attNameFrom(prop) } -> ") }
				.reduceLeft { _.append(_, ", ") }
			ComputedProperty("valueProperties", propsPart.references + Reference.valueConversions, isOverridden = true)(
				s"import $className._",
				s"Vector($quotedId -> id, $propsPart)"
			)
		}
	}
	
	private def withMethodNameFor(prop: Named)(implicit naming: NamingRules): String = withMethodNameFor(prop.name)
	private def withMethodNameFor(name: Name)(implicit naming: NamingRules) = (withMethodPrefix + name).prop
	
	private def withPropertyMethods(property: Property, calledMethodName: String = "apply",
	                                returnDescriptionStart: String = "A model containing only the specified ")
	                               (implicit naming: NamingRules) =
	{
		val concreteProp = property.concrete
		concreteProp.oneOrManyDbVariants match {
			// Case: The property matches a single column => generates one withX -method
			case Left(dbProp) => Some(withDbPropertyMethod(dbProp, concreteProp.description,
				calledMethodName = calledMethodName, returnDescriptionStart = returnDescriptionStart))
			// Case: The property matches multiple columns => generates partial and full withX method
			// variants
			case Right(dbProps) =>
				// TODO: The current system doesn't know what the concrete variants of the "parts" are and so can't
				//  write a function that accepts a concrete variant of such a type
				/*
				val extraDescription = s", which is part of the property ${ concreteProp.name }"
				dbProps.map { withDbPropertyMethod(_, returnDescriptionAppendix = extraDescription,
					calledMethodName = calledMethodName, returnDescriptionStart = returnDescriptionStart) } :+
					withMethod(concreteProp, dbProps, concreteProp.dataType.toScala, concreteProp.description,
						s" (sets all ${dbProps.size} values)", calledMethodName,
						returnDescriptionStart)
				 */
				Vector(withMethod(concreteProp, dbProps, concreteProp.dataType.toScala, concreteProp.description,
					s" (sets all ${dbProps.size} values)", calledMethodName,
					returnDescriptionStart))
		}
	}
	private def withDbPropertyMethod(property: DbProperty, paramDescription: String = "",
	                                 returnDescriptionAppendix: String = "", calledMethodName: String = "apply",
	                                 returnDescriptionStart: String = "A model containing only the specified ")
	                                (implicit naming: NamingRules) =
		withMethod(property, Vector(property), property.conversion.origin, paramDescription, returnDescriptionAppendix,
			calledMethodName, returnDescriptionStart)
	private def withMethod(source: Named, properties: Seq[DbProperty], parameterType: ScalaType,
	                       paramDescription: String = "", returnDescriptionAppendix: String = "",
	                       calledMethodName: String = "apply",
	                       returnDescriptionStart: String = "A model containing only the specified ")
	                      (implicit naming: NamingRules) =
	{
		val paramName = source.name.prop
		val constructionParamsCode = properties
			.map { prop => s"${prop.name.prop} = " +: prop.conversion.midConversion(paramName) }
			.reduceLeft { _.append(_, ", ") }
		MethodDeclaration(withMethodNameFor(source), constructionParamsCode.references,
			returnDescription = s"$returnDescriptionStart${ source.name.doc }$returnDescriptionAppendix")(
			scala.Parameter(paramName, parameterType, description = paramDescription))(
			s"$calledMethodName($constructionParamsCode)")
	}
	
	
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
				ImmutableValue("deprecationAttName", isOverridden = true)(prop.dbProperties.head.modelName.quoted))
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
			Vector(ImmutableValue("deprecationAttName", isOverridden = true)(prop.dbProperties.head.modelName.quoted))
		override def methods = Set()
	}
	
	private case class CombinedDeprecation(expirationPropName: Name, deprecationPropName: Name)
		extends DeprecationStyle
	{
		override def extensionFor(dbModelClass: ScalaType) = Reference.deprecatable
		
		override def properties(implicit naming: NamingRules) = Vector(
			ComputedProperty("nonDeprecatedCondition", Set(Reference.valueConversions, Reference.now),
				isOverridden = true)(
				s"${columnNameFrom(deprecationPropName)}.isNull && ${columnNameFrom(expirationPropName)} > Now")
		)
		override def methods = Set()
	}
}
