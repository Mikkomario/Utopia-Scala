package utopia.reach.coder.controller.writer

import utopia.coder.model.data.{Name, NamingRules, ProjectSetup}
import utopia.coder.model.scala.datatype.Reference._
import utopia.coder.model.scala.datatype.TypeVariance.Covariance
import utopia.coder.model.scala.datatype.{Extension, GenericType, Reference, ScalaType, TypeRequirement}
import utopia.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue}
import utopia.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration, PropertyDeclaration, TraitDeclaration}
import utopia.coder.model.scala.{DeclarationDate, Parameter}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.reach.coder.model.data.{ComponentFactory, Property}
import utopia.reach.coder.model.enumeration.ContextType
import utopia.reach.coder.util.ReachReferences.Reach._

/**
  * Used for writing component factory classes
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.0
  */
object ComponentFactoryWriter
{
	// ATTRIBUTES -----------------------
	
	private lazy val repr = GenericType("Repr", variance = Covariance,
		description = "Implementing factory/settings type")
	private lazy val reprType = repr.toScalaType
	
	
	// OTHER    -----------------------
	
	/**
	  * Writes component creation factory traits, classes and objects
	  * @param factory Factory definitions
	  * @param naming Naming logic used
	  * @param setup Implicit project settings
	  * @return Reference to the generated file. Failure if writing failed.
	  */
	def apply(factory: ComponentFactory)(implicit naming: NamingRules, setup: ProjectSetup) = {
		val componentType = ScalaType.basic(factory.componentName.className)
		
		val settingsName = (factory.componentName + "Settings").className
		
		val settingsLikeDec = settingsLike(factory)
		val settingsLikeType = settingsLikeDec.toBasicType
		
		val settingsCompanionDec = settingsCompanion(settingsName)
		val settingsDec = settings(factory, settingsName, settingsLikeType)
		val settingsType = settingsDec.toBasicType
		
		val settingsWrapperDec = settingsWrapper(factory, settingsLikeType, settingsType)
		val settingsWrapperType = settingsWrapperDec.toBasicType
		
		// The FactoryLike trait is only written if there are two or more inheritors
		val factoryLikeDec = {
			if (factory.onlyContextual || factory.contextType.isEmpty)
				None
			else
				Some(factoryLike(factory, componentType, settingsWrapperType))
		}
		val factoryLikeType = factoryLikeDec match {
			case Some(f) => f.toBasicType
			case None => settingsWrapperType
		}
		
		val contextualDec = factory.contextType.map { context =>
			contextualFactory(factory, ("Contextual" +: (factory.componentName + "Factory")).className, context,
				componentType, settingsType, factoryLikeType) -> context
		}
		val nonContextualDec = {
			if (factory.onlyContextual)
				None
			else
				Some(nonContextualFactory(factory, componentType, settingsType, factoryLikeType,
					contextualDec.map { case (contextual, context) => contextual.toBasicType -> context }))
		}
		
		val setupDec = {
			if (nonContextualDec.isEmpty && contextualDec.isEmpty)
				None
			else
				Some(setupFactory(factory, settingsType, settingsWrapperType, nonContextualDec.map { _.toBasicType },
					contextualDec.map { case (contextual, context) => (contextual.toBasicType, context) }))
		}
		val companionDec = setupDec.map { setupDec => companion(factory, settingsType, setupDec.toBasicType) }
		
		File(
			factory.pck,
			Vector(settingsLikeDec, settingsCompanionDec, settingsDec, settingsWrapperDec) ++ factoryLikeDec ++
				contextualDec.map { _._1 } ++ nonContextualDec ++ setupDec ++ companionDec,
			factory.componentName.className, Set[Reference]()
		).write()
	}
	
	// Declares the generic settings trait
	private def settingsLike(factory: ComponentFactory)(implicit naming: NamingRules, setup: ProjectSetup) = {
		// Adds additional properties and methods for nested settings
		val (referencedProps, referencedMethods) = factory.properties.splitFlatMap { prop =>
			prop.reference.iterator.splitFlatMap { case (factory, prefix) =>
				referencedPropFunctionsFrom(prop, factory, prefix)
			}
		}
		TraitDeclaration(
			name = (factory.componentName + "SettingsLike").className,
			genericTypes = Vector(repr),
			// Allows custom extensions
			extensions = factory.parentTraits.map { _.toExtension(reprType) } ++ referenceExtensionsIteratorFor(factory),
			// Defines abstract properties
			properties = factory.properties.map { prop =>
				PropertyDeclaration.newAbstract(prop.name.function, prop.dataType, description = prop.description)
			} ++ referencedProps,
			// Defines abstract property setters, as well as mapping functions based on each setter
			methods = factory.properties.flatMap { prop =>
				val setterName = prop.setterName.function
				val setter = MethodDeclaration.newAbstract(setterName, reprType,
					description = prop.description,
					returnDescription = s"Copy of this factory with the specified ${ prop.name.doc }",
					isLowMergePriority = true)(
					Parameter(prop.setterParamName.prop, prop.dataType,
						description = s"New ${ prop.name.doc } to use. \n${prop.description}"))
				// Case: Mapping is enabled => Writes both the setter and the mapper
				if (prop.mappingEnabled) {
					val mapper = MethodDeclaration(("map" +: prop.name).function, isLowMergePriority = true)(
						Parameter("f", flow.mutate(prop.dataType)))(
						s"$setterName(f(${prop.name.function}))")
					Pair(setter, mapper)
				}
				// Case: Mapping is disabled => Writes the setter only
				else
					Some(setter)
			}.toSet ++ referencedMethods,
			description = s"Common trait for ${factory.componentName} factories and settings",
			author = factory.author,
			since = DeclarationDate.versionedToday
		)
	}
	
	private def settingsCompanion(name: String) =
		ObjectDeclaration(name, properties = Vector(ImmutableValue("default")("apply()")))
	
	// Declares the settings case class
	private def settings(factory: ComponentFactory, name: String, settingsLikeType: ScalaType)
	                    (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		val settingsType = ScalaType.basic(name)
		ClassDeclaration(
			name = name,
			// Defines all properties in the constructor
			constructionParams = factory.allProperties.map { prop =>
				Parameter(prop.name.prop, prop.dataType, prop.defaultValue, description = prop.description)
			},
			extensions = Vector(settingsLikeType(settingsType)),
			// Specifies a setter for each property
			methods = factory.allProperties.map { prop =>
				val paramName = prop.setterParamName.prop
				MethodDeclaration(prop.setterName.function, isOverridden = true, isLowMergePriority = true)(
					Parameter(paramName, prop.dataType))(
					s"copy(${prop.name.function} = $paramName)")
			}.toSet,
			description = s"Combined settings used when constructing ${factory.componentName.pluralDoc}",
			author = factory.author,
			since = DeclarationDate.versionedToday,
			isCaseClass = true
		)
	}
	
	// Creates a wrapper trait for the settings
	private def settingsWrapper(factory: ComponentFactory, settingsLikeType: ScalaType, settingsType: ScalaType)
	                           (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		TraitDeclaration(
			name = (factory.componentName + "SettingsWrapper").className,
			genericTypes = Vector(repr),
			extensions = Vector(settingsLikeType(reprType)),
			// Defines an abstract settings property
			properties = PropertyDeclaration
				.newAbstract("settings", settingsType, description = "Settings wrapped by this instance",
					isProtected = true) +:
				// Also defines all property getters
				factory.allProperties.map { prop =>
					val propName = prop.name.prop
					ComputedProperty(propName, isOverridden = true)(s"settings.$propName")
				},
			// Defines setters for each property
			methods = factory.allProperties.map { prop =>
				val setterName = prop.setterName.function
				val paramName = prop.setterParamName.prop
				MethodDeclaration(setterName, isOverridden = true, isLowMergePriority = true)(
					Parameter(paramName, prop.dataType))(s"mapSettings { _.$setterName($paramName) }")
			}.toSet +
				// Also defines abstract settings setter function
				MethodDeclaration.newAbstract("withSettings", reprType,
					returnDescription = "Copy of this factory with the specified settings")(
					Parameter("settings", settingsType)) +
				// Defines a mapper function for modifying settings
				MethodDeclaration("mapSettings")(
					Parameter("f", settingsType.fromParameters(settingsType)))(
					"withSettings(f(settings))"),
			description = s"Common trait for factories that wrap a ${factory.componentName} settings instance",
			author = factory.author,
			since = DeclarationDate.versionedToday
		)
	}
	
	// Creates the common component factory trait
	private def factoryLike(factory: ComponentFactory, componentType: ScalaType, settingsWrapperType: ScalaType)
	                       (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		TraitDeclaration(
			name = (factory.componentName + "FactoryLike").className,
			genericTypes = Vector(repr),
			extensions = (settingsWrapperType(reprType): Extension) +:
				factory.containerType.map[Extension] { _.factoryTrait(componentType, componentLike) }.toVector :+
				partOfHierarchy,
			description = s"Common trait for factories that are used for constructing ${factory.componentName.pluralDoc}",
			author = factory.author,
			since = DeclarationDate.versionedToday
		)
	}
	
	// Creates the non-contextual factory variant
	private def nonContextualFactory(factory: ComponentFactory, componentType: ScalaType, settingsType: ScalaType,
	                                 factoryLikeType: ScalaType, contextual: Option[(ScalaType, ContextType)] = None)
	                                (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		val name = (factory.componentName + "Factory").className
		val factoryType = ScalaType.basic(name)
		// Creates the extension and function for contextual version -accessing, if appropriate
		val contextualParentAndMethod = contextual.map { case (contextual, context) =>
			// If variable (pointer) context type is used, extends a different trait
			// Containers also use different traits and methods
			if (factory.isContainer) {
				val parent = fromGenericContextFactory(context.reference, contextual)
				val method = MethodDeclaration("withContext",
					genericTypes = Vector(
						GenericType("N", Some(TypeRequirement.childOf(context.reference)))
					), isOverridden = true)(
					Parameter("context", ScalaType.basic("N")))(
					s"$contextual(parentHierarchy, context, settings)")
				(parent, method)
			}
			else if (factory.useVariableContext) {
				val method = MethodDeclaration("withContextPointer", isOverridden = true)(
					Parameter("p", flow.changing(context.reference)))(
					s"$contextual(parentHierarchy, p, settings)")
				fromVariableContextFactory(context.reference, contextual) -> method
			}
			else {
				val withContextMethod = MethodDeclaration("withContext", isOverridden = true)(
					Parameter("context", context.reference))(
					s"$contextual(parentHierarchy, context, settings)")
				fromContextFactory(context.reference, contextual) -> withContextMethod
			}
		}
		ClassDeclaration(
			name = name,
			// Contains the standard (implemented) properties + non-context -specific properties
			constructionParams = Vector(
				Parameter("parentHierarchy", componentHierarchy),
				Parameter("settings", settingsType, s"$settingsType.default")
			) ++ factory.nonContextualProperties.map { prop =>
				Parameter(prop.name.prop, prop.dataType, prop.defaultValue, description = prop.description)
			},
			extensions = ((factoryLikeType(factoryType): Extension) +:
				// Enables context-appending, if specified
				contextualParentAndMethod.map[Extension] { _._1 }.toVector) ++
				factory.containerType.map[Extension] { _.nonContextualFactoryTrait(componentType, componentLike) },
			methods = Set(
				// Settings setter -function
				MethodDeclaration("withSettings", isOverridden = true)(
					Parameter("settings", settingsType))("copy(settings = settings)")) ++
				// Context append -function (optional)
				contextualParentAndMethod.map { _._2 } ++
				factory.nonContextualProperties.map { prop =>
					val paramName = prop.setterParamName.prop
					MethodDeclaration(prop.setterName.function,
						returnDescription = s"Copy of this factory with the specified ${prop.name}")(
						Parameter(paramName, prop.dataType, description = prop.description))(
						s"copy(${prop.name.prop} = $paramName)")
				},
			description = s"Factory class that is used for constructing ${
				factory.componentName.pluralDoc} without using contextual information",
			author = factory.author,
			since = DeclarationDate.versionedToday,
			isCaseClass = true
		)
	}
	
	// Creates the contextual factory variant
	// TODO: In container classes, the "Repr" contains [N]
	private def contextualFactory(factory: ComponentFactory, name: String, context: ContextType,
	                              componentType: ScalaType, settingsType: ScalaType, factoryLikeType: ScalaType)
	                             (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		val factoryType = ScalaType.basic(name)
		// If variable contexts are used, extends a different factory class,
		// different constructor parameter and different withContext -function
		// These properties are also different for containers
		val (contextualParent, contextParameter, withContextMethod) = {
			factory.containerType match {
				case Some(containerType) =>
					val genericContextType = ScalaType.basic("N")
					val parent = containerType.contextualFactoryTrait(genericContextType, context.reference,
						componentType, componentLike, factoryType)
					val parameter = Parameter("context", genericContextType)
					val method = MethodDeclaration("withContext",
						genericTypes = Vector(GenericType("N2", Some(TypeRequirement.childOf(context.reference)))),
						isOverridden = true)(parameter)("copy(context = context)")
					(parent, parameter, method)
				case None =>
					if (factory.useVariableContext) {
						val parentTrait = variableContextualFactory(context.reference, factoryType)
						val parameter = Parameter("contextPointer", flow.changing(context.reference))
						val withContextMethod = MethodDeclaration("withContextPointer", isOverridden = true)(parameter)(
							"copy(contextPointer = contextPointer)")
						(parentTrait, parameter, withContextMethod)
					}
					else {
						val parentTrait = context.factory(factoryType)
						val parameter = Parameter("context", context.reference)
						val withContextMethod = MethodDeclaration("withContext", isOverridden = true)(parameter)(
							"copy(context = context)")
						(parentTrait, parameter, withContextMethod)
					}
			}
		}
		// Factories use generic context
		val genericTypes = {
			if (factory.isContainer)
				Vector(GenericType("N", Some(TypeRequirement.childOf(context.reference)), Covariance,
					description = "Type of context used and passed along by this factory"))
			else
				Vector()
		}
		// If no "FactoryLike" trait is applied, extends PartOfComponentHierarchy
		val hierarchyExtension = if (factory.onlyContextual) Some(partOfHierarchy: Extension) else None
		ClassDeclaration(
			name = name,
			genericTypes = genericTypes,
			// Contains the default parameters (parentHierarchy, context, settings),
			// plus possible custom properties
			constructionParams = Vector(
				Parameter("parentHierarchy", componentHierarchy),
				contextParameter,
				Parameter("settings", settingsType, s"$settingsType.default")
			) ++ factory.contextualProperties.map { prop =>
				Parameter(prop.name.prop, prop.dataType, prop.defaultValue, description = prop.description)
			},
			// TODO: Add support for contextual ReachFactoryTrait variants
			extensions = Vector[Extension](factoryLikeType(factoryType), contextualParent) ++ hierarchyExtension,
			properties = Vector(ComputedProperty("self", isOverridden = true)("this")),
			// Implements the required setters
			methods = Set(
				withContextMethod,
				MethodDeclaration("withSettings", isOverridden = true)(
					Parameter("settings", settingsType))("copy(settings = settings)")
			) ++
				// Also contains possible custom property setters
				factory.contextualProperties.map { prop =>
					val paramName = prop.setterParamName.prop
					MethodDeclaration(prop.setterName.function,
						returnDescription = s"Copy of this factory with the specified ${prop.name}")(
						Parameter(paramName, prop.dataType, description = prop.description))(
						s"copy(${prop.name.prop} = $paramName)")
				},
			description = s"Factory class used for constructing ${
				factory.componentName.pluralDoc} using contextual component creation information",
			author = factory.author,
			since = DeclarationDate.versionedToday,
			isCaseClass = true
		)
	}
	
	// Creates the outside-hierarchy setup class
	// TODO: Containers need different parents and methods
	private def setupFactory(factory: ComponentFactory, settingsType: ScalaType, settingsWrapperType: ScalaType,
	                         nonContextualFactoryType: Option[ScalaType],
	                         contextualFactoryType: Option[(ScalaType, ContextType)])
	                        (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		val name = (factory.componentName + "Setup").className
		val setupType = ScalaType.basic(name)
		
		// If non-contextual factory version is disabled, specifies different extension and methods
		val hierarchyParam = Parameter("hierarchy", componentHierarchy)
		val nonContextualParentAndMethod = nonContextualFactoryType.map { nonContextual =>
			cff(nonContextual) -> MethodDeclaration("apply", isOverridden = true)(
				hierarchyParam)(s"$nonContextual(hierarchy, settings)")
		}
		val contextualParentAndMethod = contextualFactoryType.map { case (contextual, context) =>
			// Variable context specifies a different method and parent
			if (factory.useVariableContext) {
				val method = MethodDeclaration("withContextPointer", isOverridden = true)(
					Vector(hierarchyParam, Parameter("p", flow.changing(context.reference))))(
					s"$contextual(hierarchy, p, settings)")
				vccff(context.reference, contextual) -> method
			}
			else {
				val method = MethodDeclaration("withContext", isOverridden = true)(
					Vector(hierarchyParam, Parameter("context", context.reference)))(
					s"$contextual(hierarchy, context, settings)")
				ccff(context.reference, contextual) -> method
			}
		}
		
		ClassDeclaration(
			name = (factory.componentName + "Setup").className,
			constructionParams = Vector(Parameter("settings", settingsType, s"$settingsType.default")),
			extensions = Vector[Extension](settingsWrapperType(setupType)) ++
				nonContextualParentAndMethod.map[Extension] { _._1 } ++
				contextualParentAndMethod.map[Extension] { _._1 },
			// Implements the required methods
			methods = Set(MethodDeclaration("withSettings", isOverridden = true)(
				Parameter("settings", settingsType))("copy(settings = settings)")) ++
				nonContextualParentAndMethod.map { _._2 } ++ contextualParentAndMethod.map { _._2 },
			description = s"Used for defining ${
				factory.componentName} creation settings outside of the component building process",
			author = factory.author,
			since = DeclarationDate.versionedToday,
			isCaseClass = true
		)
	}
	
	// Creates the component companion object that serves as the root access point
	private def companion(factory: ComponentFactory, settingsType: ScalaType, setupType: ScalaType)
	                     (implicit naming: NamingRules, setup: ProjectSetup) =
	{
		ObjectDeclaration(
			name = factory.componentName.className,
			extensions = Vector(Extension(setupType, Vector(Vector()))),
			// Provides a public apply function for settings-assignment
			methods = Set(MethodDeclaration("apply")(Parameter("settings", settingsType))("withSettings(settings)")),
			author = factory.author,
			since = DeclarationDate.versionedToday
		)
	}
	
	// Extensions are only enabled for reference properties that don't use prefixed properties
	private def referenceExtensionsIteratorFor(factory: ComponentFactory)(implicit naming: NamingRules): Iterator[Extension] = {
		factory.properties.iterator.flatMap { prop =>
			if (prop.prefixDerivedProperties)
				Iterator.empty
			else
				prop.reference.iterator.flatMap { case (factory, _) =>
					val ref: Extension = Reference(factory.pck,
						(factory.componentName + "SettingsLike").className)(reprType)
					ref +: referenceExtensionsIteratorFor(factory)
				}
		}
	}
	
	private def referencedPropFunctionsFrom(referenceProperty: Property, target: ComponentFactory, prefix: Name,
	                                        allowNonPrefixed: Boolean = true)
	                                       (implicit naming: NamingRules): (Vector[PropertyDeclaration], Vector[MethodDeclaration]) =
	{
		val usePrefixes = allowNonPrefixed && referenceProperty.prefixDerivedProperties
		val base = referenceProperty.name.prop
		target.allProperties.splitFlatMap { prop =>
			// Generates a getter
			val propName = if (usePrefixes) prefix +: prop.name else prop.name
			val directGet = ComputedProperty(propName.prop,
				description = if (usePrefixes) s"${prop.name} from the wrapped ${target.componentName} settings" else "",
				isOverridden = !usePrefixes)(
				s"$base.${prop.name.prop}")
			// Generates a setter
			val paramName = prop.setterParamName.prop
			val setterName = if (usePrefixes) "with" +: propName else prop.setterName
			val directSet = MethodDeclaration(setterName.function,
				returnDescription = if (usePrefixes) s"Copy of this factory with the specified $propName" else "",
				isOverridden = !usePrefixes)(
				Parameter(paramName, prop.dataType, description = if (usePrefixes) prop.description else ""))(
				s"${referenceProperty.setterName.function}($base.${prop.setterName.function}($paramName))")
			// Generates a mapper, if enabled
			// Non-prefixed properties don't need a mapper because of inheritance
			val mapper = {
				if (usePrefixes && prop.mappingEnabled)
					Some(MethodDeclaration(("map" +: propName).function)(
						Parameter("f", flow.mutate(prop.dataType)))(
						s"${directSet.name}(f(${directGet.name}))"))
				else
					None
			}
			// Recursively generates more properties if the reference goes deeper
			val (moreProps, moreMethods) = prop.reference match {
				case Some((deeperTarget, nextPrefix)) =>
					val appliedPrefix = if (usePrefixes) prefix + nextPrefix else nextPrefix
					referencedPropFunctionsFrom(prop, deeperTarget, appliedPrefix, allowNonPrefixed = !usePrefixes)
				case None => Vector() -> Vector()
			}
			(directGet +: moreProps) -> ((directSet +: mapper.toVector) ++ moreMethods)
		}
	}
}
