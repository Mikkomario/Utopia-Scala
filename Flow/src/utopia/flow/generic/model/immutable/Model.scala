package utopia.flow.generic.model.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.collection.mutable.iterator.LazyInitIterator
import utopia.flow.collection.template.MapAccess
import utopia.flow.generic.factory.PropertyFactory
import utopia.flow.generic.model.immutable.Model.RenamedModel
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.generic.model.template.{HasPropertiesLike, Property, ValueConvertible}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.util.UncertainBoolean
import utopia.flow.util.UncertainBoolean.{CertainBoolean, CertainlyFalse, CertainlyTrue}

import scala.collection.mutable

object Model
{
	// COMPUTED -------------------------------
	
	/**
	 * @return An empty model
	 */
	def empty: Model = EmptyModel
	
	@deprecated("Please call HasPropertiesLike.haveSimilarProperties directly", "v2.7")
	implicit def similarProperties: EqualsFunction[HasPropertiesLike[Property]] =
		HasPropertiesLike.haveSimilarProperties
	
	
	// OTHER    -------------------------------
	
	/**
	 * @param pair A key-value pair
	 * @return A model containing that key and value
	 */
	def from[A](pair: (String, A))(implicit valueConversion: A => ValueConvertible): Model =
		new _Model(Single(Constant(pair._1, valueConversion(pair._2).toValue)))
	/**
	 * @param pair A key-value pair
	 * @return A model containing that key and value
	 */
	def from(pair: (String, Value)): Model = new _Model(Single(Constant(pair._1, pair._2)))
	/**
	 * @param first The first key-value pair
	 * @param second The second key-value pair
	 * @param more Additional key-value pairs
	 * @return A model containing the specified key-value pairs
	 */
	def from(first: (String, Value), second: (String, Value), more: (String, Value)*): Model =
		new _Model(OptimizedIndexedSeq.from(Pair(first, second).view ++ more).map { case (k, v) => Constant(k, v) })
	
	/**
	 * @param properties Key value pairs to assign to this model
	 * @return A new model with the specified properties
	 */
	def apply(properties: IterableOnce[(String, Value)]): Model = properties match {
		case v: scala.collection.View[(String, Value)] =>
			if (v.knownSize == 0)
				EmptyModel
			else
				new LazyModel(v.map { case (name, value) => Constant(name, value) })
		case s: CachingSeq[(String, Value)] =>
			if (s.isEmpty)
				EmptyModel
			else
				new LazyModel(s.map { case (name, value) => Constant(name, value) })
			
		case i: Iterable[(String, Value)] =>
			if (i.isEmpty)
				EmptyModel
			else
				new _Model(OptimizedIndexedSeq.from(i.view.map { case (name, value) => Constant(name, value) }))
			
		case i =>
			i.nonEmptyIterator match {
				case Some(iterator) => new LazyModel(iterator.map { case (name, value) => Constant(name, value) })
				case None => EmptyModel
			}
	}
	
	/**
	 * @param only The only constant to store in this model
	 * @return A model containing the specified constant
	 */
	def withConstants(only: Constant): Model = new _Model(Single(only))
	/**
	 * @param first First constant to store
	 * @param second Second constant to store
	 * @param more More constants to include
	 * @return A model with the specified constants
	 */
	def withConstants(first: Constant, second: Constant, more: Constant*): Model =
		new _Model(Pair(first, second) ++ more)
	/**
	 * @param constants Constants from which this model consists
	 * @return A new model based on the specified constants
	 */
	def withConstants(constants: IterableOnce[Constant]): Model = constants match {
		case s: CachingSeq[Constant] => if (s.isEmpty) EmptyModel else new LazyModel(s)
		case s: Seq[Constant] => if (s.isEmpty) EmptyModel else new _Model(s)
		case v: scala.collection.View[Constant] => if (v.knownSize == 0) EmptyModel else new LazyModel(v)
		case i: Iterable[Constant] => if (i.isEmpty) EmptyModel else new _Model(OptimizedIndexedSeq.from(i))
		case i => if (i.knownSize == 0) EmptyModel else new LazyModel(i)
	}
	/**
	 * @param original The original model
	 * @param declaration Applied model declaration
	 * @param prevalidatedProperties Properties from 'original' that have already been processed / validated (optional).
	 * @return Access to the original model's properties through the specified declaration
	 */
	def validated(original: HasProperties, declaration: ModelDeclaration,
	              prevalidatedProperties: Iterable[(PropertyDeclaration, Constant)] = Empty): Model =
		new ValidatedModel(original, declaration, prevalidatedProperties)
	
	/**
	 * Converts another type of model into this type of model
	 * @param model A model
	 * @return An immutable model, based on the specified instance
	 */
	def from(model: HasPropertiesLike[Property]): Model = model match {
		case m: Model => m
		case m =>
			withConstants(m.properties.map {
				case c: Constant => c
				case p => Constant(p.name, p.value)
			})
	}
	
	/**
	 * Converts a map of valueConvertible elements into a model format.
	 * @param content The map that is converted to model attributes
	 * @return The newly generated model
	 */
	def fromMap[C1](content: Map[String, C1])(implicit f: C1 => ValueConvertible) =
		withConstants(content.view.map { case (name, value) => Constant(name, value.toValue) })
	
	/**
	 * @param properties Properties to assign to this model, as key-value pairs
	 * @param factory A factory used for generating new properties
	 * @return A new model that may generate additional properties
	 */
	@deprecated("Deprecated for possible removal. Generative models don't currently yield all their properties, when iterated.", "v2.7")
	def apply(properties: IterableOnce[(String, Value)], factory: PropertyFactory[Constant]): Model =
		new GenerativeModel(OptimizedIndexedSeq.from(properties.iterator.map { case (k, v) => Constant(k, v) }), factory)
	/**
	 * @param constants Properties to assign to this model
	 * @param factory A factory used for generating new properties
	 * @return A new model that may generate additional properties
	 */
    @deprecated("Deprecated for possible removal. Generative models don't currently yield all their properties, when iterated.", "v2.7")
	def withConstants(constants: IterableOnce[Constant], factory: PropertyFactory[Constant]): Model =
		new GenerativeModel(OptimizedIndexedSeq.from(constants), factory)
	/**
	 * Converts a map of valueConvertible elements into a model format.
	 * The generator the model uses may be specified as well.
	 * @param content The map that is converted to model attributes
	 * @param propFactory the property generator that will generate all the properties
	 * @return The newly generated model
	 */
	@deprecated("Deprecated for possible removal. Generative models don't currently yield all their properties, when iterated.", "v2.7")
	def fromMap[C1](content: Map[String, C1], propFactory: PropertyFactory[Constant])
	               (implicit f: C1 => ValueConvertible): Model =
		new GenerativeModel(OptimizedIndexedSeq.from(content.iterator.map { case (k, v) => propFactory(k, v.toValue) }),
			propFactory)
	
	
	// NESTED   -------------------------------
	
	/**
	 * An empty model implementation
	 */
	object EmptyModel extends Model
	{
		// ATTRIBUTES   -----------------------
		
		override val properties: Seq[Constant] = Empty
		
		
		// IMPLEMENTED  -----------------------
		
		override def self: Model = this
		
		override def propertiesIterator: Iterator[Constant] = Iterator.empty
		
		override def existingProperty(propName: String): Option[Constant] = None
		
		override def contains(propName: String): Boolean = false
		override def containsNonEmpty(propName: String): Boolean = false
		override def knownContains(propName: String): UncertainBoolean = CertainlyFalse
	}
	
	private class _Model(override val properties: Seq[Constant]) extends Model
	{
		// ATTRIBUTES   -----------------------
		
		private lazy val propMap = propertiesIterator.map { p => p.name.toLowerCase -> p }.toMap
		
		
		// IMPLEMENTED  -----------------------
		
		override def self: Model = this
		
		override def propertiesIterator: Iterator[Constant] = properties.iterator
		
		override def existingProperty(propName: String): Option[Constant] = propMap.get(propName.toLowerCase)
		
		override def contains(propName: String): Boolean = propMap.contains(propName.toLowerCase)
		override def containsNonEmpty(propName: String): Boolean = existingProperty(propName).exists { _.nonEmpty }
		override def knownContains(propName: String): UncertainBoolean = contains(propName)
	}
	
	private class LazyModel(props: IterableOnce[Constant]) extends Model
	{
		// ATTRIBUTES   -----------------------
		
		override lazy val properties: Seq[Constant] = props match {
			case s: Seq[Constant] => s
			case v: scala.collection.View[Constant] => CachingSeq.from(v)
			case i: Iterable[Constant] => OptimizedIndexedSeq.from(i)
			case i => CachingSeq.from(i.iterator)
		}
		private lazy val propMap = new BuildingPropertyMap(propertiesIterator)
		
		
		// IMPLEMENTED  -----------------------
		
		override def self: Model = this
		
		override def propertiesIterator: Iterator[Constant] = properties.iterator
		
		override def contains(propName: String): Boolean = propMap(propName).isDefined
		override def containsNonEmpty(propName: String): Boolean = propMap(propName).exists { _.value.nonEmpty }
		override def knownContains(propName: String): UncertainBoolean = propMap.knownContains(propName)
		
		override def existingProperty(propName: String): Option[Constant] = propMap(propName)
	}
	
	private class RenamedModel(source: Model, renames: PropertyRenames) extends Model
	{
		// ATTRIBUTES   ---------------------
		
		override lazy val properties: Seq[Constant] = source.propertiesIterator
			.map { p =>
				renames.renamedOption(p.name) match {
					case Some(newName) => p.withName(newName)
					case None => p
				}
			}
			.caching
		
		
		// IMPLEMENTED  ---------------------
		
		override def self: Model = this
		
		override def propertiesIterator: Iterator[Constant] = properties.iterator
		
		override def contains(propName: String): Boolean = renames.yields(propName) || source.contains(propName)
		override def containsNonEmpty(propName: String): Boolean = existingProperty(propName).exists { _.nonEmpty }
		override def knownContains(propName: String): UncertainBoolean =
			CertainBoolean(renames.yields(propName)) || source.knownContains(propName)
		
		override def existingProperty(propName: String): Option[Constant] =
			renames.originalOption(propName) match {
				case Some(original) => source.existingProperty(original)
				case None => if (renames.refersTo(propName)) None else source.existingProperty(propName)
			}
		
		override def +(renames: PropertyRenames) = new RenamedModel(source, this.renames ++ renames)
	}
	
	private class ValidatedModel(source: HasProperties, declaration: ModelDeclaration,
	                             prevalidated: Iterable[(PropertyDeclaration, Constant)])
		extends Model
	{
		import ModelDeclaration.valueIsEmpty
		
		// ATTRIBUTES   ---------------------
		
		private val generatedMappings = scala.collection.mutable.Map[PropertyDeclaration, Constant]()
		generatedMappings ++= prevalidated
		
		// Lazily combines the 3 sources of properties:
		//      1. Prevalidated properties
		//      2. Other source properties
		//      3. Declared properties that were not present in the source model
		override val properties: Seq[Constant] = CachingSeq(
			source = source.propertiesIterator
				.flatMap { original =>
					if (prevalidated.exists { _._2 == original })
						None
					else
						declaration.find(original.name) match {
							case Some(declaration) =>
								if (generatedMappings.contains(declaration))
									None
								else {
									val constant = makeConstant(declaration, original.name, original.value)
									generatedMappings += (declaration -> constant)
									Some(constant)
								}
							case None => Some(Constant.from(original))
						}
				} ++
				LazyInitIterator {
					declaration.declarations.iterator.filterNot(generatedMappings.contains)
						.map { declaration => Constant(declaration.name, declaration.defaultValue) }
				},
			preCached = prevalidated.iterator.map { _._2 }.toVector)
		private lazy val propMap = new BuildingPropertyMap(propertiesIterator)
		
		
		// IMPLEMENTED  ---------------------
		
		override def self: Model = this
		
		override def propertiesIterator: Iterator[Constant] = properties.iterator
		
		override def existingProperty(propName: String): Option[Constant] = propMap(propName)
		
		override def contains(propName: String): Boolean = declaration.contains(propName) || propMap(propName).isDefined
		override def containsNonEmpty(propName: String): Boolean = propMap(propName).exists { _.nonEmpty }
		override def knownContains(propName: String): UncertainBoolean =
			propMap.knownContains(propName) || declaration.contains(propName)
		
		
		// OTHER    ---------------------------
		
		private def makeConstant(declaration: PropertyDeclaration, originalName: String, value: Value) = {
			// Order of priority is: 1) Specified value, 2) Value from the source model, 3) Declared default value
			val appliedValue = value.notEmpty
				.flatMap { _.castTo(declaration.dataType).filterNot(valueIsEmpty) }
				.orElse {
					val alternativeNames = declaration.names.filterNot { _ ~== originalName }
					if (alternativeNames.isEmpty)
						None
					else
						source(alternativeNames).notEmpty
							.flatMap { _.castTo(declaration.dataType).filterNot(valueIsEmpty) }
				}
				.getOrElse(declaration.defaultValue)
			
			Constant(declaration.name, appliedValue)
		}
	}
	
	/**
	 * A Model implementation matching (more or less) the previous version.
	 * Used for backwards compatibility. Not intended for continuous use.
	 * @param initialProperties Properties assigned initially
	 * @param generator A factory for constructing new properties.
	 */
	private class GenerativeModel(initialProperties: Seq[Constant], generator: PropertyFactory[Constant])
		extends Model
	{
		// ATTRIBUTES   ------------------------
		
		private var _properties = initialProperties
		private var propMap = initialProperties.iterator.map { p => p.name.toLowerCase -> p }.toMap
		
		
		// IMPLEMENTED  ------------------------
		
		override def self: Model = this
		
		override def properties: Seq[Constant] = _properties
		override def propertiesIterator: Iterator[Constant] = _properties.iterator
		
		protected override def equalsProperties = Iterator.single(generator) ++ propertiesIterator
		
		override def existingProperty(propName: String): Option[Constant] = propMap.get(propName.toLowerCase)
		override def property(propName: String): Constant =
			existingProperty(propName).getOrElse { generateProp(propName) }
		
		override protected def simulateValueFor(propName: String): Value = generateProp(propName).value
		
		override def contains(propName: String): Boolean =
			propMap.contains(propName.toLowerCase) || generator.generatesNonEmpty(propName)
		override def containsNonEmpty(propName: String): Boolean =
			existingProperty(propName).exists { _.nonEmpty } || generator.generatesNonEmpty(propName)
		
		override def knownContains(propName: String): UncertainBoolean =
			if (propMap.contains(propName.toLowerCase)) CertainlyTrue else UncertainBoolean
		
		override def withProperties(properties: IterableOnce[Constant]): Model =
			new GenerativeModel(OptimizedIndexedSeq.from(properties), generator)
		
		
		// OTHER    ---------------------------
		
		private def generateProp(propName: String) = {
			val prop = generator(propName)
			_properties :+= prop
			propMap += (prop.name.toLowerCase -> prop)
			prop
		}
	}
	
	private class BuildingPropertyMap(propsIter: Iterator[Constant])
		extends MapAccess[String, Option[Constant]] with mutable.Growable[(String, Constant)]
	{
		// ATTRIBUTES   -----------------------
		
		private val map = scala.collection.mutable.Map[String, Constant]()
		
		
		// IMPLEMENTED  -----------------------
		
		override def apply(key: String): Option[Constant] = {
			// Takes the cached value, if available
			val lowerKey = key.toLowerCase
			map.get(lowerKey).orElse {
				// If not available, iterates the remaining values until a match is found
				var result: Option[Constant] = None
				while (result.isEmpty && propsIter.hasNext) {
					val p = propsIter.next()
					val lowerName = p.name.toLowerCase
					// Caches all iterated values
					map += (lowerName -> p)
					if (lowerName == lowerKey)
						result = Some(p)
				}
				result
			}
		}
		
		override def addOne(elem: (String, Constant)): BuildingPropertyMap.this.type = {
			map += (elem._1.toLowerCase -> elem._2)
			this
		}
		override def clear(): Unit = map.clear()
		
		
		// OTHER    ----------------------------
		
		def knownContains(propName: String): UncertainBoolean = {
			if (map.contains(propName.toLowerCase))
				CertainlyTrue
			else if (propsIter.hasNext)
				UncertainBoolean
			else
				CertainlyFalse
		}
	}
}

/**
 * Common trait for immutable models
 * @author Mikko Hilpinen
 * @since 23.10.2025, v2.7
 */
trait Model extends ModelLike[Model]
{
	override def withProperties(properties: IterableOnce[Constant]): Model = Model.withConstants(properties)
	
	override def +(renames: PropertyRenames): Model = new RenamedModel(this, renames)
}