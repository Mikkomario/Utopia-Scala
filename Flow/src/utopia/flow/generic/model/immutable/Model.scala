package utopia.flow.generic.model.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.collection.mutable.iterator.LazyInitIterator
import utopia.flow.collection.template.MapAccess
import utopia.flow.generic.factory.PropertyFactory
import utopia.flow.generic.model.immutable.Model.{AppendingModel, RenamedModel, SwappingModel, withConstants}
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.generic.model.template.{HasPropertiesLike, Property, ValueConvertible}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.util.UncertainBoolean.{CertainBoolean, CertainlyFalse, CertainlyTrue}
import utopia.flow.util.{Mutate, UncertainBoolean}
import utopia.flow.view.immutable.caching.Lazy

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
		new SinglePropModel(Constant(pair._1, valueConversion(pair._2).toValue))
	/**
	 * @param pair A key-value pair
	 * @return A model containing that key and value
	 */
	def from(pair: (String, Value)): Model = new SinglePropModel(Constant(pair._1, pair._2))
	/**
	 * @param first The first key-value pair
	 * @param second The second key-value pair
	 * @param more Additional key-value pairs
	 * @return A model containing the specified key-value pairs
	 */
	def from(first: (String, Value), second: (String, Value), more: (String, Value)*): Model =
		new _Model(OptimizedIndexedSeq.from((Pair(first, second).view ++ more).map { case (k, v) => Constant(k, v) }))
	
	/**
	 * @param properties Key value pairs to assign to this model
	 * @return A new model with the specified properties
	 */
	def apply(properties: IterableOnce[(String, Value)]): Model = properties match {
		// Case: The properties are computed lazily => Constructs a lazy model (unless empty)
		case v: scala.collection.View[(String, Value)] =>
			v.knownSize match {
				case 0 => EmptyModel
				case 1 => new LazySinglePropModel(Lazy { Constant(v.head) })
				case _ => new LazyModel(v.map { case (name, value) => Constant(name, value) })
			}
		// Case: The properties are cached lazily => Constructs a lazy model (unless empty or size of 1)
		case s: CachingSeq[(String, Value)] =>
			if (s.isEmpty)
				EmptyModel
			else if (s.hasSize(1))
				new SinglePropModel(Constant(s.head))
			else
				new LazyModel(s.map { case (name, value) => Constant(name, value) })
		// Case: Properties may be iterated multiple times (assumes that they're cached as well)
		//       => Constructs a fully cached model
		case i: Iterable[(String, Value)] =>
			if (i.isEmpty)
				EmptyModel
			else if (i.hasSize(1))
				new SinglePropModel(Constant(i.head))
			else
				new _Model(OptimizedIndexedSeq.from(i.view.map { case (name, value) => Constant(name, value) }))
		// Case: Lazily iterated collection => Caches it lazily
		case i =>
			i.nonEmptyIterator match {
				case Some(iterator) =>
					if (iterator.knownSize == 1)
						new LazySinglePropModel(Lazy { Constant(iterator.next()) })
					else
						new LazyModel(iterator.map { case (name, value) => Constant(name, value) })
				case None => EmptyModel
			}
	}
	
	/**
	 * @param only The only constant to store in this model
	 * @return A model containing the specified constant
	 */
	def withConstants(only: Constant): Model = new SinglePropModel(only)
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
		case s: CachingSeq[Constant] =>
			if (s.isEmpty)
				EmptyModel
			else if (s.isFullyCached)
				new _Model(s)
			else if (s.hasSize(1))
				new SinglePropModel(s.head)
			else
				new LazyModel(s)
		case s: Seq[Constant] =>
			s.emptyOneOrMany match {
				case None => EmptyModel
				case Some(Left(only)) => new SinglePropModel(only)
				case Some(Right(props)) => new _Model(props)
			}
		case v: scala.collection.View[Constant] =>
			v.knownSize match {
				case 0 => EmptyModel
				case 1 => new LazySinglePropModel(Lazy { v.head })
				case _ => new LazyModel(v)
			}
		case i: Iterable[Constant] =>
			i.emptyOneOrMany match {
				case None => EmptyModel
				case Some(Left(only)) => new SinglePropModel(only)
				case Some(Right(props)) => new _Model(OptimizedIndexedSeq.from(props))
			}
		case i =>
			i.knownSize match {
				case 0 => EmptyModel
				case 1 => new LazySinglePropModel(Lazy { i.iterator.next() })
				case _ => new LazyModel(i)
			}
	}
	/**
	 * @param original The original model
	 * @param declaration Applied model declaration
	 * @param prevalidatedProperties Properties from 'original' that have already been processed / validated (optional).
	 * @param noDefaultValues Set this to true in order to disable the adding of missing properties and property values
	 *                        from the specified declaration.
	 *
	 *                        If left to false (default), all declared properties that don't appear in 'original'
	 *                        will be added and populated with their default values,
	 *                        and all declared properties with an empty value
	 *                        will be populated by their default value instead.
	 * @return Access to the original model's properties through the specified declaration
	 */
	def validated(original: HasProperties, declaration: ModelDeclaration,
	              prevalidatedProperties: Iterable[(PropertyDeclaration, Constant)] = Empty,
	              noDefaultValues: Boolean = false): Model =
		new ValidatedModel(original, declaration, prevalidatedProperties, !noDefaultValues)
	
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
		
		override def sorted: Model = this
		override def withoutEmptyValues: Model = this
		
		override def existingProperty(propName: String): Option[Constant] = None
		
		override def contains(propName: String): Boolean = false
		override def containsNonEmpty(propName: String): Boolean = false
		override def knownContains(propName: String): UncertainBoolean = CertainlyFalse
		
		override def +(renames: PropertyRenames): Model = this
		override def +(prop: Constant): Model = new SinglePropModel(prop)
		override def +:(prop: Constant): Model = new SinglePropModel(prop)
		
		override def ++(props: IterableOnce[Constant]): Model = Model.withConstants(props)
		override def ++(other: HasPropertiesLike[Constant]): Model = Model.from(other)
		
		override def -(prop: Property): Model = this
		override def -(propName: String): Model = this
		override def without(propName: String): Model = this
		override def --(propNames: IterableOnce[String]): Model = this
		override def without(keys: IterableOnce[String]): Model = this
		override def withoutProperties(properties: Set[Constant]): Model = this
		
		override def filter(f: Constant => Boolean): Model = this
		override def filterNot(f: Constant => Boolean): Model = this
		
		override def renamed(renames: IterableOnce[Pair[String]]): Model = this
		override def renamed(oldName: String, newName: String): Model = this
		
		override def sortBy[A](f: Constant => A)(implicit ord: Ordering[A]): Model = this
		
		override def map(f: Mutate[Constant]): Model = this
		override def mapKeys(f: Mutate[String]): Model = this
		override def mapValues(f: Mutate[Value]): Model = this
		
		override def map(propName: String, requireExisting: Boolean)(f: Mutate[Constant]): Model = {
			if (requireExisting)
				this
			else
				this + f(Constant(propName, Value.empty))
		}
		override def mapValue(propName: String, requireExisting: Boolean)(f: Mutate[Value]): Model = {
			if (requireExisting)
				this
			else
				this + Constant(propName, f(Value.empty))
		}
		override def addComputed(propName: String, newName: String, replace: Boolean, requireExisting: Boolean)
		                        (mapValue: Mutate[Value]): Model =
		{
			if (requireExisting)
				this
			else
				this + Constant(newName, mapValue(Value.empty))
		}
	}
	
	private class SinglePropModel(property: Constant) extends Model
	{
		// ATTRIBUTES   --------------------
		
		override lazy val properties: Seq[Constant] = Single(property)
		
		
		// IMPLEMENTED  --------------------
		
		override def self: Model = this
		
		override def propertiesIterator: Iterator[Constant] = Iterator.single(property)
		
		override def sorted: Model = this
		
		override def contains(propName: String): Boolean = property.name ~== propName
		override def knownContains(propName: String): UncertainBoolean = contains(propName)
		override def containsNonEmpty(propName: String): Boolean =
			property.nonEmpty && (property.name ~== propName)
		
		override def existingProperty(propName: String): Option[Constant] =
			Some(property).filter { _.name ~== propName }
		
		override def +(prop: Constant): Model = Model.withConstants(Pair(property, prop))
		override def +:(prop: Constant): Model = Model.withConstants(Pair(prop, property))
		
		override def -(prop: Property): Model = if (property == prop) EmptyModel else this
		override def -(propName: String): Model = if (property.name ~== propName) EmptyModel else this
		override def --(propNames: IterableOnce[String]): Model =
			if (propNames.iterator.exists { _ ~== property.name }) EmptyModel else this
		
		override def filter(f: Constant => Boolean): Model = if (f(property)) this else EmptyModel
		
		override def sortBy[A](f: Constant => A)(implicit ord: Ordering[A]): Model = this
		
		override def map(f: Mutate[Constant]): Model = new SinglePropModel(f(property))
		override def mapKeys(f: Mutate[String]): Model = new SinglePropModel(property.mapName(f))
		override def mapValues(f: Mutate[Value]): Model = new SinglePropModel(property.mapValue(f))
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
	
	private class LazySinglePropModel(lazyProp: Lazy[Constant]) extends Model
	{
		// ATTRIBUTES   ---------------------
		
		override lazy val properties: Seq[Constant] = Single(prop)
		
		
		// COMPUTED ------------------------
		
		private def prop = lazyProp.value
		
		
		// IMPLEMENTED  ---------------------
		
		override def self: Model = this
		
		override def propertiesIterator: Iterator[Constant] = lazyProp.valueIterator
		
		override def contains(propName: String): Boolean = prop.name ~== propName
		override def containsNonEmpty(propName: String): Boolean = {
			val p = prop
			p.nonEmpty && (p.name ~== propName)
		}
		override def knownContains(propName: String): UncertainBoolean = lazyProp.current match {
			case Some(prop) => CertainBoolean(prop.name ~== propName)
			case None => UncertainBoolean
		}
		
		override def existingProperty(propName: String): Option[Constant] =
			Some(prop).filter { _.name ~== propName }
		
		override def sortBy[A](f: Constant => A)(implicit ord: Ordering[A]): Model = this
		
		override def map(f: Mutate[Constant]): Model = lazyProp.current match {
			case Some(prop) => new SinglePropModel(f(prop))
			case None => new LazySinglePropModel(lazyProp.lightMap(f))
		}
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
	
	// NB: Assumes that no overlap exists between source and added
	private class AppendingModel(source: Model, added: Iterable[Constant]) extends Model
	{
		// ATTRIBUTES   ---------------------
		
		private val lazyAddedPropMap = Lazy { added.iterator.map { p => p.name.toLowerCase -> p }.toMap }
		override lazy val properties: Seq[Constant] = source.properties ++ added
		
		
		// IMPLEMENTED  ---------------------
		
		override def self: Model = this
		
		override def propertiesIterator: Iterator[Constant] = source.propertiesIterator ++ added
		
		override def contains(propName: String): Boolean = lazyAddedPropMap.current match {
			case Some(map) => map.contains(propName.toLowerCase) || source.contains(propName)
			case None => source.contains(propName) || lazyAddedPropMap.value.contains(propName.toLowerCase)
		}
		override def containsNonEmpty(propName: String): Boolean =
			lazyAddedPropMap.value.get(propName.toLowerCase) match {
				case Some(prop) => prop.nonEmpty
				case None => source.containsNonEmpty(propName)
			}
		override def knownContains(propName: String): UncertainBoolean =
			source.knownContains(propName) || (lazyAddedPropMap.current match {
				case Some(map) => CertainBoolean(map.contains(propName.toLowerCase))
				case None => UncertainBoolean
			})
		
		override def existingProperty(propName: String): Option[Constant] = lazyAddedPropMap.current match {
			case Some(map) => map.get(propName.toLowerCase).orElse(source.existingProperty(propName))
			case None => source.existingProperty(propName).orElse { lazyAddedPropMap.value.get(propName.toLowerCase) }
		}
	}
	/**
	 * Wraps another model, modifying some of its properties
	 * @param source The original model
	 * @param added Added properties (as a Model)
	 * @param removed Names of 'source' properties (lower-case), which should not appear in 'properties'
	 */
	private class SwappingModel(source: Model, added: Model, removed: Set[String]) extends Model
	{
		// ATTRIBUTES   ----------------------
		
		override val properties: Seq[Constant] =
			CachingSeq.from(source.propertiesIterator.filterNot { p => removed.contains(p.name.toLowerCase) } ++
				added.propertiesIterator)
		
		
		// IMPLEMENTED  ----------------------
		
		override def self: Model = this
		
		override def propertiesIterator: Iterator[Constant] = properties.iterator
		
		override def contains(propName: String): Boolean = added.contains(propName) || source.contains(propName)
		override def containsNonEmpty(propName: String): Boolean = existingProperty(propName).exists { _.nonEmpty }
		override def knownContains(propName: String): UncertainBoolean =
			added.knownContains(propName) || source.knownContains(propName)
		
		override def existingProperty(propName: String): Option[Constant] =
			added.existingProperty(propName).orElse { source.existingProperty(propName) }
	}
	
	private class ValidatedModel(source: HasProperties, declaration: ModelDeclaration,
	                             prevalidated: Iterable[(PropertyDeclaration, Constant)], defaultsEnabled: Boolean)
		extends Model
	{
		import ModelDeclaration.valueIsEmpty
		
		// ATTRIBUTES   ---------------------
		
		private val generatedMappings = scala.collection.mutable.Map[PropertyDeclaration, Constant]()
		generatedMappings ++= prevalidated
		
		// Lazily combines the 3 sources of properties:
		//      1. Prevalidated properties
		//      2. Other source properties
		//      3. Declared properties that were not present in the source model (optional)
		override val properties: Seq[Constant] = {
			val fromSourceIterator = source.propertiesIterator
				.flatMap { original =>
					// Case: This property was already introduced => Skips it
					if (prevalidated.exists { _._2 == original })
						None
					else
						declaration.find(original.name) match {
							// Case: Declared property => May modify the property, or ignore it
							case Some(declaration) =>
								// Case: Already specified => Skips it
								if (generatedMappings.contains(declaration))
									None
								else {
									val constant = makeConstant(declaration, original.name, original.value)
									generatedMappings += (declaration -> constant)
									Some(constant)
								}
							// Case: Undeclared property => Keeps it as it is
							case None => Some(Constant.from(original))
						}
				}
			CachingSeq(
				source = {
					if (defaultsEnabled)
						fromSourceIterator ++ LazyInitIterator {
							declaration.declarations.iterator.filterNot(generatedMappings.contains)
								.map { declaration => Constant(declaration.name, declaration.defaultValue) }
						}
					else
						fromSourceIterator
				},
				preCached = prevalidated.iterator.map { _._2 }.toVector)
		}
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
			// Order of priority is:
			//      1. Specified 'value'
			//      2. Value from the source model
			//      3. Declared default value (which may be disabled)
			val appliedValue = value.notEmpty
				.flatMap { _.castTo(declaration.dataType).filterNot(valueIsEmpty) }
				// 2.
				.orElse {
					val alternativeNames = declaration.names.filterNot { _ ~== originalName }
					if (alternativeNames.isEmpty)
						None
					else
						source(alternativeNames).notEmpty
							.flatMap { _.castTo(declaration.dataType).filterNot(valueIsEmpty) }
				}
				// 3.
				.getOrElse { if (defaultsEnabled) declaration.defaultValue else Value.empty }
			
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
	
	/**
	 * A lazily building property name to property value -map, based on an iterator of constants
	 * @param propsIter Iterator that yields the properties to map
	 */
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
		
		/**
		 * @param propName Name of the targeted property (case-insensitive)
		 * @return Whether this map contains that property.
		 *         Uncertain, if further iteration would be required in order to know for certain.
		 */
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
	
	override def +(prop: Constant): Model = {
		if (isEmpty)
			Model.withConstants(prop)
		else if (knownContains(prop.name).mayBeTrue)
			new SwappingModel(this, Model.withConstants(prop), Set(prop.name.toLowerCase))
		else
			new AppendingModel(this, Single(prop))
	}
	override def ++(props: IterableOnce[Constant]): Model = {
		if (isEmpty)
			withConstants(props)
		else
			props match {
				case v: scala.collection.View[Constant] => super.++(v)
				case i: Iterable[Constant] =>
					i.emptyOneOrMany match {
						case None => self
						case Some(Left(only)) => this + only
						case Some(Right(many)) =>
							val propNames = many.iterator.map { _.name.toLowerCase }.toSet
							if (propNames.forall { knownContains(_).isCertainlyFalse })
								new AppendingModel(this, many)
							else
								new SwappingModel(this, Model.withConstants(many), propNames)
					}
				case i => super.++(i)
			}
	}
	
	override def map(propName: String, requireExisting: Boolean)(f: Mutate[Constant]): Model = {
		if (knownContains(propName).isCertainlyFalse) {
			if (requireExisting)
				self
			else
				this + f(Constant(propName, simulateValueFor(propName)))
		}
		else
			existingProperty(propName) match {
				case Some(prop) =>
					val newProp = f(prop)
					new SwappingModel(this, Model.withConstants(newProp),
						Set(propName.toLowerCase, newProp.name.toLowerCase))
					
				case None =>
					if (requireExisting)
						self
					else
						this + f(Constant(propName, simulateValueFor(propName)))
			}
	}
	override def addComputed(propName: String, newName: String, replace: Boolean, requireExisting: Boolean)
	                        (mapValue: Mutate[Value]): Model =
	{
		if (requireExisting)
			super.addComputed(propName, newName, replace, requireExisting)(mapValue)
		else
			new SwappingModel(this, Model.withConstants(Constant.lazily(newName, Lazy { mapValue(apply(propName)) })),
				Set(propName.toLowerCase, newName.toLowerCase))
	}
}