package utopia.reach.coder.controller.reader

import utopia.bunnymunch.jawn.JsonBunny
import utopia.coder.model.data.{Name, NamingRules}
import utopia.coder.model.enumeration.NameContext.{ClassName, ClassPropName, FunctionName}
import utopia.coder.model.enumeration.NamingConvention
import utopia.coder.model.enumeration.NamingConvention.CamelCase
import utopia.coder.model.scala.Package
import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.datatype.{Reference, ScalaType}
import utopia.flow.collection.mutable.builder.{CompoundingMapBuilder, CompoundingVectorBuilder}
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.DataType.{ModelType, VectorType}
import utopia.flow.util.StringExtensions._
import utopia.flow.util.Version
import utopia.reach.coder.model.data.{ComponentFactory, ProjectData, Property}
import utopia.reach.coder.model.enumeration.{ContextType, ReachFactoryTrait}

import java.nio.file.Path

/**
  * Used for reading component factory data from json
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.0
  */
object ComponentFactoryReader
{
	/**
	  * Parses project and component data from the specified path
	  * @param path Path to the project json file
	  * @return Parsed project data. Failure if json parsing failed.
	  */
	def apply(path: Path) = JsonBunny(path).map { rootV =>
		implicit val naming: NamingRules = NamingRules.default
		val root = rootV.getModel
		// Parses standard project information
		val projectName = root("project").stringOr("Project")
		val version = root("version").string.flatMap { Version.findFrom }
		val author = root("author").getString
		val basePackage = Package(root("package").getString)
		// Parses package aliases and reference aliases
		val packageAliases = packageAliasesFrom(root("package_aliases", "packages").getModel)
		val referenceAliases = referenceAliasesFrom(root("reference_aliases", "references", "aliases").getModel,
			packageAliases)
		// Parses the component factories
		val factoriesBuilder = new CompoundingVectorBuilder[ComponentFactory]()
		parseComponentFactoriesFrom(root("components").getModel, basePackage, factoriesBuilder, packageAliases,
			referenceAliases, author)
		
		ProjectData(projectName, factoriesBuilder.result(), version)
	}
	
	private def packageAliasesFrom(aliases: Model) = {
		val builder = new CompoundingMapBuilder[String, Package]()
		// Each property represents a single package alias
		aliases.properties.foreach { prop =>
			val name = prop.name
			val pck = prop.value.getString
			// Case: / is used to refer to another package alias
			if (pck.contains('/')) {
				val (referred, afterRef) = pck.splitAtFirst("/")
				val fullPackage = builder.get(referred) match {
					// Case: Referred alias could be discerned from previously specified aliases
					case Some(parent) => parent/afterRef
					// Case: Referred alias couldn't be discerned => Prints a warning
					case None =>
						println(s"Warning: Reference '$referred' couldn't be determined for package alias '$name'")
						Package(s"$referred.$afterRef")
				}
				builder += (name -> fullPackage)
			}
			// Case: This alias doesn't make references
			else
				builder += (name -> Package(pck))
		}
		builder.result()
	}
	
	private def referenceAliasesFrom(aliases: Model, packageAliases: Map[String, Package]) = {
		// Each property represents a reference alias
		aliases.properties.map { prop =>
			val name = prop.name
			val ref = prop.value.getString
			val reference = {
				// Case: The reference refers to a package alias
				if (ref.contains('/')) {
					val (referred, afterRef) = ref.splitAtFirst("/")
					packageAliases.get(referred) match {
						// Case: Package alias could be resolved
						case Some(parentPackage) => Reference(s"$parentPackage.$afterRef")
						// Case: Undefined package alias => Shows a warning
						case None =>
							println(s"Warning: Package reference '$referred' couldn't be determined for alias '$name'")
							Reference(s"$referred.$afterRef")
					}
				}
				// Case: Reference without a package reference
				else
					Reference(ref)
			}
			name -> reference
		}.toMap
	}
	
	private def parseComponentFactoriesFrom(packagesModel: Model, parentPackage: Package,
	                                        builder: CompoundingVectorBuilder[ComponentFactory],
	                                        packageAliases: Map[String, Package],
	                                        referenceAliases: Map[String, Reference], projectAuthor: String)
	                                       (implicit naming: NamingRules): Unit =
	{
		// Each property is expected to represent a package
		// Each property may either contain
		//      1) Another package object, or
		//      2) An array of component factory objects
		packagesModel.properties.foreach { prop =>
			val pck = parentPackage/prop.name
			prop.value.castTo(ModelType, VectorType) match {
				// Case: Package model => Parses the model contents
				case Left(packageModelV) =>
					parseComponentFactoriesFrom(packageModelV.getModel, pck, builder, packageAliases,
						referenceAliases, projectAuthor)
				// Case: An array of factory models => Parses the models and adds them to the builder
				case Right(factoryArrayV) =>
					factoryArrayV.getVector.foreach { v =>
						val factory = componentFactoryFrom(v.getModel, pck, packageAliases, referenceAliases,
							projectAuthor) { factoryName => builder.find { _.componentName ~== factoryName } }
						builder += factory
					}
			}
		}
	}
	
	private def componentFactoryFrom(model: Model, parentPackage: Package, packageAliases: Map[String, Package],
	                                 referenceAliases: Map[String, Reference], projectAuthor: String)
	                                (factoryReferences: String => Option[ComponentFactory])
	                                (implicit naming: NamingRules) =
	{
		ComponentFactory(
			pck = parentPackage,
			componentName = ClassName.from(model)
				.getOrElse(Name.interpret("UnnamedComponent", CamelCase.capitalized)),
			contextType = model("context").string.flatMap { input =>
				val result = ContextType(input)
				if (result.isEmpty)
					println(s"Warning: No context type matches '$input'")
				result
			},
			parentTraits = model("parents", "parent").getVector.flatMap { _.string.flatMap { input =>
				val result = ReachFactoryTrait(input)
				if (input.isEmpty)
					println(s"Warning: No Reach factory trait matches '$input'")
				result
			} },
			properties = model("properties", "props").getVector
				.flatMap { _.model.map { propertyFrom(_, packageAliases, referenceAliases)(factoryReferences) } },
			nonContextualProperties = model("nonContextualProps", "non_contextual_props").getVector
				.flatMap { _.model.map { propertyFrom(_, packageAliases, referenceAliases)(factoryReferences) } },
			contextualProperties = model("contextualProps", "contextual_props").getVector
				.flatMap { _.model.map { propertyFrom(_, packageAliases, referenceAliases)(factoryReferences) } },
			author = model("author").stringOr(projectAuthor),
			onlyContextual = model("onlyContextual", "only_contextual").getBoolean,
			useVariableContext = model("variable_context", "variableContext", "view", "variable").getBoolean
		)
	}
	
	private def propertyFrom(model: Model, packageAliases: Map[String, Package],
	                         referenceAliases: Map[String, Reference])
	                        (factoryReferences: String => Option[ComponentFactory])
	                        (implicit naming: NamingRules) =
	{
		val description = model("description", "doc").getString
		// Checks whether the property refers to another component, or whether it is a standard property
		model("reference", "ref", "factory", "settings").string match {
			// Case: Reference property => Attempts to resolve the reference
			case Some(reference) =>
				factoryReferences(reference) match {
					// Case: Reference resolved => Forms a property
					case Some(factory) =>
						val prefix = model("prefix").string match {
							case Some(prefix) =>
								model("prefix_plural").string match {
									case Some(plural) =>
										Name(prefix, plural, NamingConvention.of(prefix, naming(ClassPropName)))
									case None => Name.interpret(prefix, naming(ClassPropName))
								}
							case None => Name.empty
						}
						Property.referringTo(factory, prefix, description,
							model("prefix_properties", "prefix_props").booleanOr(true))
					// Case: Reference couldn't be resolved => Warns and forms a placeholder property
					case None =>
						println(s"Couldn't resolve component factory reference '$reference'")
						Property.simple(reference, ScalaType.basic(reference))
				}
			// Case: Standard property
			case None =>
				val name: Name = ClassPropName.from(model).getOrElse { "unnamedProperty" }
				val setterName = model("setter").string match {
					case Some(custom) => Name.interpret(custom, naming(FunctionName))
					case None => "with" +: name
				}
				val paramName = model("param").string match {
					case Some(custom) => Name.interpret(custom, naming(ClassPropName))
					case None => name
				}
				Property(name, scalaTypeFrom(model("type").getString, packageAliases, referenceAliases),
					setterName, paramName,
					CodePiece.fromValue(model("default"), packageAliases, referenceAliases).getOrElse(CodePiece.empty),
					None, description, model("mapping_enabled", "mapping", "map").getBoolean)
		}
	}
	
	private def scalaTypeFrom(typeString: String, packageAliases: Map[String, Package],
	                          referenceAliases: Map[String, Reference]): ScalaType =
	{
		// Case: Generic type => Parses the parent and the child types separately and then combines them
		if (typeString.contains('[')) {
			val (parentTypeString, childTypeString) = typeString.splitAtFirst("[")
			scalaTypeFrom(parentTypeString, packageAliases, referenceAliases)(
				scalaTypeFrom(childTypeString.untilLast("]"), packageAliases, referenceAliases))
		}
		// Case: Non-generic type
		else
			referenceAliases.get(typeString) match {
				// Case: Alias used => Resolves the alias
				case Some(reference) => reference
				case None =>
					// Case: Package alias used => Resolves the package alias and forms a complete reference
					if (typeString.contains('/')) {
						val (packageRefPart, typePart) = typeString.splitAtFirst("/")
						packageAliases.get(packageRefPart) match {
							case Some(parentPackage) => Reference(s"$parentPackage.$typePart")
							// Case: Package alias couldn't be resolved => Warns the user
							case None =>
								println(s"Warning: Package reference '$packageRefPart' couldn't be resolved for type '$typeString'")
								Reference(s"$packageRefPart.$typePart")
						}
					}
					// Case: Reference type
					else if (typeString.contains('.'))
						Reference(typeString)
					// Case: Basic scala type
					else
						ScalaType.basic(typeString)
			}
	}
}
