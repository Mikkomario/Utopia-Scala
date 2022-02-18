package utopia.vault.coder.controller.writer.database

import utopia.flow.datastructure.immutable.Pair
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Name, NamingRules, ProjectSetup}
import utopia.vault.coder.model.enumeration.NamingConvention.CamelCase
import utopia.vault.coder.model.scala.Visibility.{Private, Protected}
import utopia.vault.coder.model.scala.datatype.TypeVariance.Covariance
import utopia.vault.coder.model.scala.datatype.{Extension, GenericType, Reference, ScalaType}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, DeclarationStart, File, MethodDeclaration, ObjectDeclaration, PropertyDeclaration, TraitDeclaration}
import utopia.vault.coder.model.scala.{DeclarationDate, Package, Parameter, Parameters}

import scala.io.Codec
import scala.util.Success

/**
  * Used for writing database access templates
  * @author Mikko Hilpinen
  * @since 2.9.2021, v0.1
  */
object AccessWriter
{
	// ATTRIBUTES   ------------------------------
	
	private val accessPrefix = Name("Db", "Db", CamelCase.capitalized)
	private val singleAccessPrefix = accessPrefix + "Single"
	
	private val manyPrefix = Name("Many", "Many", CamelCase.capitalized)
	
	private val accessTraitSuffix = Name("Access", "Access", CamelCase.capitalized)
	private val genericAccessSuffix = Name("Like", "Like", CamelCase.capitalized)
	private val subViewSuffix = Name("SubView", "SubView", CamelCase.capitalized)
	private val uniqueAccessPrefix = Name("Unique", "Unique", CamelCase.capitalized)
	
	private val newPrefix = Name("new", "new", CamelCase.lower)
	
	private lazy val connectionParam = Parameter("connection", Reference.connection)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param c A class
	  * @return Name of the single id access point for that class
	  */
	def singleIdAccessNameFor(c: Class)(implicit naming: NamingRules) = (singleAccessPrefix +: c.name).className
	
	/**
	  * @param c     A class
	  * @param setup Project setup (implicit)
	  * @return Package that contains singular access points for that class
	  */
	def singleAccessPackageFor(c: Class)(implicit setup: ProjectSetup) = setup.singleAccessPackage / c.packageName
	
	/**
	  * @param c     A class
	  * @param setup Project setup (implicit)
	  * @return Reference to the access point for unique instances of that class based on their id
	  */
	def singleIdReferenceFor(c: Class)(implicit setup: ProjectSetup, naming: NamingRules) =
		Reference(singleAccessPackageFor(c), singleIdAccessNameFor(c))
	
	/**
	  * Writes database access point objects and traits
	  * @param classToWrite          Class based on which these access points are written
	  * @param modelRef              Reference to the stored model class
	  * @param factoryRef            Reference to the from DB factory object
	  * @param dbModelRef            Reference to the database model class
	  * @param descriptionReferences References to the described model version + single description link access point +
	  *                              many description links access point, if applicable for this class
	  * @param codec                 Implicit codec to use when writing files
	  * @param setup                 Implicit project-specific setup to use
	  * @return References to<br>
	  *         1) trait common for distinct single model access points<br>
	  *         2) Single model access point object<br>
	  *         3) Trait common for many model access points<br>
	  *         4) Many model access point object
	  */
	def apply(classToWrite: Class, modelRef: Reference, factoryRef: Reference, dbModelRef: Reference,
	          descriptionReferences: Option[(Reference, Reference, Reference)])
	         (implicit codec: Codec, setup: ProjectSetup, naming: NamingRules) =
	{
		// Standard access point properties (factory, model)
		// are the same for both single and many model access points
		val baseProperties = Vector(
			ComputedProperty("factory", Set(factoryRef), isOverridden = true)(factoryRef.target),
			ComputedProperty("model", Set(dbModelRef), Protected,
				description = "Factory used for constructing database the interaction models")(dbModelRef.target)
		)
		// For classes that support deprecation, deprecate() -method is added for all traits
		// (implementation varies, however)
		// Option[Pair[method]], where first method is for individual access and second for many access
		val deprecationMethods = classToWrite.deprecationProperty.map { prop =>
			Pair(prop.name.propName, prop.name.pluralPropName).map { propName =>
				MethodDeclaration("deprecate", Set(Reference.now, Reference.valueConversions),
					description = s"Deprecates all accessible ${classToWrite.name.pluralText}",
					returnDescription = "Whether any row was targeted")(
					Parameters(Vector(Vector()), Vector(connectionParam)))(s"$propName = Now")
			}
		}
		// Root access points extend either the UnconditionalView or the NonDeprecatedView -trait,
		// depending on whether deprecation is supported
		val rootViewExtension: Extension = {
			if (classToWrite.isDeprecatable)
				Reference.nonDeprecatedView(modelRef)
			else
				Reference.unconditionalView
		}
		
		writeSingleAccesses(classToWrite, modelRef, descriptionReferences, baseProperties,
			deprecationMethods.map { _.first }, rootViewExtension)
			.flatMap { case (singleAccessRef, uniqueAccessRef, _) =>
				writeManyAccesses(classToWrite, modelRef, descriptionReferences, baseProperties,
					deprecationMethods.map { _.second }, rootViewExtension)
					.map { case (manyAccessRef, manyAccessTraitRef) =>
						(uniqueAccessRef, singleAccessRef, manyAccessTraitRef, manyAccessRef)
					}
			}
	}
	
	// Writes all single item access points
	// Returns Try[(SingleAccessRef, UniqueAccessRef, SingleIdAccessRef)]
	private def writeSingleAccesses(classToWrite: Class, modelRef: Reference,
	                                descriptionReferences: Option[(Reference, Reference, Reference)],
	                                baseProperties: Vector[PropertyDeclaration],
	                                deprecationMethod: Option[MethodDeclaration], rootViewExtension: Extension)
	                               (implicit naming: NamingRules, codec: Codec, setup: ProjectSetup) =
	{
		val singleAccessPackage = singleAccessPackageFor(classToWrite)
		writeUniqueAccess(classToWrite, modelRef, singleAccessPackage, baseProperties, deprecationMethod)
			.flatMap { uniqueAccessRef =>
				writeSingleIdAccess(classToWrite, modelRef, uniqueAccessRef, descriptionReferences, singleAccessPackage)
					.flatMap { singleIdAccessRef =>
						writeSingleRootAccess(classToWrite, modelRef, singleIdAccessRef, singleAccessPackage,
							baseProperties, rootViewExtension)
							.map { singleRootAccessRef =>
								(singleRootAccessRef, uniqueAccessRef, singleIdAccessRef)
							}
					}
			}
	}
	
	private def writeUniqueAccess(classToWrite: Class, modelRef: Reference, singleAccessPackage: Package,
	                              baseProperties: Vector[PropertyDeclaration],
	                              deprecationMethod: Option[MethodDeclaration])
	                             (implicit naming: NamingRules, codec: Codec, setup: ProjectSetup) =
	{
		val uniqueAccessName = ((uniqueAccessPrefix +: classToWrite.name) + accessTraitSuffix).className
		val pullIdCode = classToWrite.idType.nullable.fromValueCode(s"pullColumn(index)")
		
		File(singleAccessPackage,
			TraitDeclaration(uniqueAccessName,
				// Extends SingleRowModelAccess, DistinctModelAccess and Indexed
				extensions = Vector(Reference.singleRowModelAccess(modelRef),
					Reference.distinctModelAccess(modelRef, ScalaType.option(modelRef), Reference.value),
					Reference.indexed),
				// Provides computed accessors for individual properties
				properties = baseProperties ++ classToWrite.properties.map { prop =>
					val pullCode = prop.dataType.nullable
						.fromValueCode(s"pullColumn(model.${ DbModelWriter.columnNameFrom(prop) })")
					ComputedProperty(prop.name.propName, pullCode.references,
						description = prop.description.notEmpty.getOrElse(s"The ${ prop.name } of this instance") +
							". None if no instance (or value) was found.", implicitParams = Vector(connectionParam))(
						pullCode.text)
				} :+ ComputedProperty("id", pullIdCode.references, implicitParams = Vector(connectionParam))(
					pullIdCode.text),
				// Contains setters for each property (singular)
				methods = propertySettersFor(classToWrite, connectionParam) { _.propName } ++ deprecationMethod,
				description = s"A common trait for access points that return individual and distinct ${
					classToWrite.name.pluralText
				}.", author = classToWrite.author, since = DeclarationDate.versionedToday
			)
		).write()
	}
	
	// Writes the single model by id access point
	// This access point is used for accessing individual items based on their id
	// The inherited trait depends on whether descriptions are supported,
	// this also affects implemented properties
	private def writeSingleIdAccess(classToWrite: Class, modelRef: Reference, uniqueAccessRef: Reference,
	                                descriptionReferences: Option[(Reference, Reference, Reference)],
	                                singleAccessPackage: Package)
	                               (implicit naming: NamingRules, codec: Codec, setup: ProjectSetup) =
	{
		val (singleIdAccessParent, singleIdAccessParentProperties) = descriptionReferences match {
			// Case: Class uses description => extends described access version with its propeties
			case Some((describedRef, singleDescRef, manyDescsRef)) =>
				val props = Vector(
					ComputedProperty("singleDescriptionAccess", Set(singleDescRef), Protected,
						isOverridden = true)(singleDescRef.target),
					ComputedProperty("manyDescriptionsAccess", Set(manyDescsRef), Protected, isOverridden = true)(
						manyDescsRef.target),
					ComputedProperty("describedFactory", Set(describedRef), Protected, isOverridden = true)(
						describedRef.target)
				)
				Extension(Reference.singleIdDescribedAccess(modelRef, describedRef)) -> props
			// Case: Descriptions are not supported => Extends SingleIdModel or its easier sub-trait
			case None =>
				if (classToWrite.useLongId) {
					val idValueCode = classToWrite.idType.toValueCode("id")
					Extension(Reference.singleIdModelAccess(modelRef)) -> Vector(
						ComputedProperty("idValue", idValueCode.references, isOverridden = true)(idValueCode.text))
				}
				else
					Extension(Reference.singleIntIdModelAccess(modelRef)) -> Vector()
		}
		File(singleAccessPackage,
			ClassDeclaration(singleIdAccessNameFor(classToWrite),
				constructionParams = Vector(Parameter("id", classToWrite.idType.toScala)),
				extensions = Vector(uniqueAccessRef, singleIdAccessParent),
				// Implements the .condition property
				properties = singleIdAccessParentProperties,
				description = s"An access point to individual ${
					classToWrite.name.pluralText }, based on their id",
				author = classToWrite.author, since = DeclarationDate.versionedToday, isCaseClass = true
			)
		).write()
	}
	
	// Writes the single model access point
	// Root access points extend either the UnconditionalView or the NonDeprecatedView -trait,
	// depending on whether deprecation is supported
	private def writeSingleRootAccess(classToWrite: Class, modelRef: Reference, singleIdAccessRef: Reference,
	                                  singleAccessPackage: Package,
	                                  baseProperties: Vector[PropertyDeclaration], rootViewExtension: Extension)
	                                 (implicit naming: NamingRules, codec: Codec, setup: ProjectSetup) =
	{
		File(singleAccessPackage,
			ObjectDeclaration((accessPrefix +: classToWrite.name).className,
				Vector(Reference.singleRowModelAccess(modelRef), rootViewExtension, Reference.indexed),
				properties = baseProperties,
				// Defines an .apply(id) method for accessing individual items
				methods = Set(MethodDeclaration("apply", Set(singleIdAccessRef),
					returnDescription = s"An access point to that ${ classToWrite.name }")(
					Parameter("id", classToWrite.idType.toScala,
						description = s"Database id of the targeted ${ classToWrite.name }"))(
					s"${ singleIdAccessRef.target }(id)")),
				description = s"Used for accessing individual ${ classToWrite.name.pluralText }",
				author = classToWrite.author, since = DeclarationDate.versionedToday
			)
		).write()
	}
	
	// Writes all access points which access multiple items at a time
	// Returns Try[(ManyRootAccessRef, ManyAccessTraitRef)]
	private def writeManyAccesses(classToWrite: Class, modelRef: Reference,
	                              descriptionReferences: Option[(Reference, Reference, Reference)],
	                              baseProperties: Vector[PropertyDeclaration],
	                              deprecationMethod: Option[MethodDeclaration], rootViewExtension: Extension)
	                             (implicit naming: NamingRules, codec: Codec, setup: ProjectSetup) =
	{
		val manyAccessPackage = setup.manyAccessPackage / classToWrite.packageName
		writeManyAccessTrait(classToWrite, modelRef, descriptionReferences, manyAccessPackage, baseProperties,
			deprecationMethod)
			.flatMap { manyAccessTraitRef =>
				writeManyRootAccess(classToWrite, modelRef, manyAccessTraitRef, descriptionReferences,
					manyAccessPackage, rootViewExtension)
					.map { manyAccessRef => (manyAccessRef, manyAccessTraitRef) }
			}
	}
	
	// Writes a trait common for the many model access points
	private def writeManyAccessTrait(classToWrite: Class, modelRef: Reference,
	                                 descriptionReferences: Option[(Reference, Reference, Reference)],
	                                 manyAccessPackage: Package, baseProperties: Vector[PropertyDeclaration],
	                                 deprecationMethod: Option[MethodDeclaration])
	                                (implicit naming: NamingRules, codec: Codec, setup: ProjectSetup) =
	{
		// Common part for all written trait names
		val traitNameBase = (manyPrefix +: classToWrite.name) + accessTraitSuffix
		
		// Properties and methods that will be written to the highest trait (which may vary)
		val highestTraitProperties = baseProperties ++
			classToWrite.properties.map { prop =>
				val pullCode = prop.dataType
					.fromValuesCode(s"pullColumn(model.${ DbModelWriter.columnNameFrom(prop) })")
				ComputedProperty(prop.name.pluralPropName, pullCode.references,
					description = s"${ prop.name.pluralText } of the accessible ${
						classToWrite.name.pluralText }",
					implicitParams = Vector(connectionParam))(pullCode.text)
			} :+
			ComputedProperty("ids", implicitParams = Vector(connectionParam))(
				s"pullColumn(index).flatMap { id => ${ classToWrite.idType.nullable.fromValueCode("id") } }")
		val highestTraitMethods = propertySettersFor(classToWrite, connectionParam) { _.pluralPropName } ++ deprecationMethod
		
		// Writes the more generic trait version (-Like) first, if one is requested
		val parentRef = {
			if (classToWrite.writeGenericAccess) {
				val item = GenericType.covariant("A")
				val repr = GenericType.childOf("Repr", Reference.manyModelAccess(item.toScalaType), Covariance)
				
				File(manyAccessPackage,
					TraitDeclaration(
						(traitNameBase + genericAccessSuffix).pluralClassName, Vector(item, repr),
						// Extends ManyModelAccess instead of ManyRowModel access because sub-traits may vary
						Vector(Reference.manyModelAccess(item.toScalaType), Reference.indexed,
							Reference.filterableView(repr.toScalaType)),
						highestTraitProperties, highestTraitMethods,
						description = s"A common trait for access points which target multiple ${
							classToWrite.name.pluralText} or similar instances at a time",
						author = classToWrite.author, since = DeclarationDate.versionedToday))
					.write().map { Some(_) }
			}
			else
				Success(None)
		}
		
		// Writes the actual access trait
		parentRef.flatMap { parentRef =>
			val traitName = traitNameBase.pluralClassName
			val traitType = ScalaType.basic(traitName)
			// TODO: Technically only the class name portion is meant to be plural
			val subViewName = ((manyPrefix +: classToWrite.name) + subViewSuffix).pluralClassName
			
			// Trait parent type depends on whether descriptions are used or not
			val (accessParent, inheritanceProperties, inheritanceMethods) = descriptionReferences match {
				case Some((describedRef, _, manyDescsRef)) =>
					val parent = Extension(Reference.manyDescribedAccess(modelRef, describedRef))
					val props = Vector(
						ComputedProperty("manyDescriptionsAccess", Set(manyDescsRef), Protected,
							isOverridden = true)(manyDescsRef.target),
						ComputedProperty("describedFactory", Set(describedRef), Protected,
							isOverridden = true)(describedRef.target)
					)
					val methods = Set(MethodDeclaration("idOf", isOverridden = true)(
						Parameter("item", modelRef))("item.id"))
					(Some(parent), props, methods)
				case None =>
					val parent = if (parentRef.isDefined) None else Some(Extension(Reference.indexed))
					(parent, Vector(), Set[MethodDeclaration]())
			}
			val parents = {
				val creationTimeParent: Option[Extension] = {
					if (classToWrite.recordsIndexedCreationTime)
						Some(Reference.chronoRowFactoryView(modelRef, traitType))
					else
						None
				}
				val rowModelAccess = Reference.manyRowModelAccess(modelRef)
				(parentRef match {
					case Some(parent) =>
						Vector[Extension](parent(modelRef, traitType), rowModelAccess) ++ creationTimeParent
					case None =>
						Vector[Extension](rowModelAccess,
							creationTimeParent.getOrElse(Reference.filterableView(traitType)))
				}) ++ accessParent
			}
			
			File(manyAccessPackage,
				ObjectDeclaration(traitName, nested = Set(
					ClassDeclaration(subViewName,
						constructionParams = Parameters(
							Parameter("parent", Reference.manyRowModelAccess(modelRef),
								prefix = Some(DeclarationStart.overrideVal)),
							Parameter("filterCondition", Reference.condition,
								prefix = Some(DeclarationStart.overrideVal))),
						extensions = Vector(traitType, Reference.subView),
						visibility = Private
					)
				)),
				TraitDeclaration(traitName,
					extensions = parents,
					// Contains computed properties to access class properties
					properties = if (parentRef.isDefined) inheritanceProperties else
						highestTraitProperties ++ inheritanceProperties,
					// Contains setters for property values (plural)
					methods = (if (parentRef.isDefined) Set[MethodDeclaration]() else highestTraitMethods) ++
						inheritanceMethods +
						MethodDeclaration("filter",
							explicitOutputType = Some(traitType),
							isOverridden = true)(
							Parameter("additionalCondition", Reference.condition))(
							s"new $traitName.$subViewName(this, additionalCondition)"),
					description = s"A common trait for access points which target multiple ${
						classToWrite.name.pluralText } at a time",
					author = classToWrite.author, since = DeclarationDate.versionedToday
				)
			).write()
		}
	}
	
	private def writeManyRootAccess(classToWrite: Class, modelRef: Reference, manyAccessTraitRef: Reference,
	                                descriptionReferences: Option[(Reference, Reference, Reference)],
	                                manyAccessPackage: Package, rootViewExtension: Extension)
	                               (implicit naming: NamingRules, codec: Codec, setup: ProjectSetup) =
	{
		val pluralClassName = classToWrite.name.pluralClassName
		// Writes the many model access point
		val manyAccessName = {
			if (classToWrite.name.className == pluralClassName)
				s"DbMany$pluralClassName"
			else
				s"Db$pluralClassName"
		}
		// There is also a nested object for id-based multi-access, which may have description support
		val subsetClassName = s"Db${ pluralClassName }Subset"
		val subSetClass = descriptionReferences match
		{
			case Some((describedRef, _, _)) =>
				ClassDeclaration(subsetClassName,
					constructionParams = Parameter("ids", ScalaType.set(ScalaType.int),
						prefix = Some(DeclarationStart.overrideVal)),
					extensions = Vector(manyAccessTraitRef,
						Reference.manyDescribedAccessByIds(modelRef, describedRef))
				)
			case None =>
				ClassDeclaration(subsetClassName,
					constructionParams = Parameter("targetIds", ScalaType.set(ScalaType.int)),
					extensions = Vector(manyAccessTraitRef),
					properties = Vector(ComputedProperty("globalCondition",
						Set(Reference.valueConversions, Reference.sqlExtensions), isOverridden = true)(
						"Some(index in targetIds)"))
				)
		}
		val subSetClassAccessMethod = MethodDeclaration("apply",
			returnDescription = s"An access point to ${
				classToWrite.name.pluralText } with the specified ids")(
			Parameter("ids", ScalaType.set(ScalaType.int),
				description = s"Ids of the targeted ${ classToWrite.name.pluralText }"))(
			s"new $subsetClassName(ids)")
		File(manyAccessPackage,
			ObjectDeclaration(manyAccessName, Vector(manyAccessTraitRef, rootViewExtension),
				methods = Set(subSetClassAccessMethod),
				nested = Set(subSetClass),
				description = s"The root access point when targeting multiple ${
					classToWrite.name.pluralText } at a time",
				author = classToWrite.author, since = DeclarationDate.versionedToday
			)
		).write()
	}
	
	private def propertySettersFor(classToWrite: Class, connectionParam: Parameter)
	                              (nameFromPropName: Name => String)
	                              (implicit naming: NamingRules) =
		classToWrite.properties.map { prop =>
			val paramName = (newPrefix +: prop.name).propName
			val paramType = prop.dataType.notNull
			val valueConversionCode = paramType.toValueCode(paramName)
			MethodDeclaration(s"${ nameFromPropName(prop.name) }_=", valueConversionCode.references,
				description = s"Updates the ${ prop.name.pluralText } of the targeted ${ classToWrite.name.pluralText }",
				returnDescription = s"Whether any ${ classToWrite.name } was affected")(
				Parameter(paramName, paramType.toScala, description = s"A new ${ prop.name } to assign")
					.withImplicits(connectionParam))(
				s"putColumn(model.${ DbModelWriter.columnNameFrom(prop) }, $valueConversionCode)")
		}.toSet
}
