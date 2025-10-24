package utopia.flow.generic.model.mutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq}
import utopia.flow.event.listener.{ChangeListener, PropertyChangeListener}
import utopia.flow.event.model.PropertyChangeEvent.{PropertyAdded, PropertyRemoved, PropertySwapped, PropertyValueChange}
import utopia.flow.event.model.{ChangeEvent, PropertyChangeEvent}
import utopia.flow.generic.factory.PropertyFactory
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.template.HasPropertiesLike

object MutableModel
{
    // TYPES    --------------------
    
    /**
      * A basic mutable model type
      */
    type AnyMutableModel = MutableModel[Variable]
    
    
    // OTHER    --------------------
    
    /**
      * Creates a new model with an existing set of properties
      * @param properties Properties to assign to this model
      * @param propFactory A property factory to use when generating new properties
      * @tparam V Type of properties stored in this model
      * @return A new model
      */
    def withVariables[V <: Variable](properties: Iterable[V], propFactory: PropertyFactory[V]) =
        new MutableModel[V](properties, propFactory)
    
    /**
      * @return An empty model
      */
    def apply() = new MutableModel(Empty, PropertyFactory.forVariables)
    
    /**
     * Creates a new model with an existing set of properties.
     * @param content The name value pairs used for generating the model's properties
     * @param propFactory A property factory to use for building the properties
     * @return A new model
     */
    def apply[V <: Variable](content: Iterable[(String, Value)], propFactory: PropertyFactory[V]): MutableModel[V] =
        withVariables(content.map { case (name, value) => propFactory(name, value) }, propFactory)
    /**
      * Creates a new model with an existing set of properties.
      * @param content The name value pairs used for generating the model's properties
      * @return A new model
      */
    def apply(content: Iterable[(String, Value)]): MutableModel[Variable] = apply(content, PropertyFactory.forVariables)
    
    /**
      * Creates a new empty model that uses the specified property factory
      * @param propFactory A property factory to use for creating new properties
      * @tparam V Type of properties within this model
      * @return An empty model
      */
    def using[V <: Variable](propFactory: PropertyFactory[V]) = new MutableModel(Empty, propFactory)
}

/**
 * This is a mutable implementation of the Model template
 * @author Mikko Hilpinen
 * @since 27.11.2016
 * @tparam V The type of variables used in this model
 * @param propFactory The variable generator used for generating new values on this model
 */
class MutableModel[V <: Variable](initialProps: Iterable[V], propFactory: PropertyFactory[V])
	extends HasPropertiesLike[V]
{
    // ATTRIBUTES    --------------
	
	private var _properties = OptimizedIndexedSeq.from(initialProps.iterator.distinctBy { _.name })
	private var propMap = _properties.iterator.map { v => v.name.toLowerCase -> v }.toMap
    
    private var _listeners: Seq[PropertyChangeListener[V]] = Empty
    
    // Stores property value listeners that are used for firing property change events
    private var valueListeners = Map[V, ChangeListener[Value]]()
    
    
    // COMPUTED -------------------
    
    /**
      * An immutable version of this model
      */
    def immutableCopy = Model.withConstants(_properties.map { p => Constant(p.name, p.value) })
    
    /**
      * The listeners that are informed of changes within this model's properties
      */
    def listeners = _listeners
    def listeners_=(newListeners: Seq[PropertyChangeListener[V]]) = {
        // May start or stop listening to property value changes
        if (_listeners.isEmpty) {
            // Case: First listener added => starts listening
            if (newListeners.nonEmpty)
                propertiesIterator.foreach(startListeningTo)
        }
        // Case: Last listener removed => stops listening
        else if (newListeners.isEmpty) {
            propertiesIterator.foreach { p => valueListeners.get(p).foreach(p.removeListener) }
            valueListeners = Map()
        }
        _listeners = newListeners
    }
    
    
    // IMPLEMENTED METHODS    -----
	
	override def properties: Seq[V] = _properties
	override def propertiesIterator: Iterator[V] = _properties.iterator
	
	override def existingProperty(propName: String): Option[V] = propMap.get(propName.toLowerCase)
	override def property(propName: String): V =
		existingProperty(propName).getOrElse { generateProperty(propName) }
	
	override protected def simulateValueFor(propName: String): Value = generateProperty(propName).value
	
	override def contains(propName: String): Boolean =
		propMap.contains(propName.toLowerCase) || propFactory.generatesNonEmpty(propName)
	override def containsNonEmpty(propName: String): Boolean =
		existingProperty(propName).exists { _.nonEmpty } || propFactory.generatesNonEmpty(propName)
	
	def propertyMap = propMap
    
    
    // OTHER    ---------------
    
    /**
     * Updates the value of a single property within this model
     * @param propName The name of the updated property
     * @param value The new value assigned to the property
     */
    def update(propName: String, value: Value) = {
        // Replaces value & generates events, may generate a new attribute
        existingProperty(propName) match {
            // Case: Existing property => updates its value
            case Some(prop) => prop.value = value
            // Case: Non-existing property => generates one
            case None => generateProperty(propName, value)
        }
    }
    
    /**
     * Adds a new property to this model.
      * If a property with the same name already exists, it is replaced with this new property
     * @param prop The property to add to this model
     */
    def +=(prop: V) = {
        val lowerName = prop.name.toLowerCase
        def informListeners(event: => PropertyChangeEvent[V]) = {
            if (listeners.nonEmpty) {
                startListeningTo(prop)
                // Generates an event, if necessary
                val e = event
                listeners.foreach { _.onPropertyChange(e) }
            }
        }
        existingProperty(lowerName) match {
            // Case: There already exists a property with that name => replaces it (generates a property change event)
            case Some(oldProp) =>
                propMap += lowerName -> prop
	            _properties = _properties.mapFirstWhere { _ == oldProp } { _ => prop }
                // May start listening to value changes
                informListeners(PropertySwapped(ChangeEvent(oldProp, prop)))
	            
            // Case: Completely new attribute => generates a property added event and modifies property order as well
            case None =>
                propMap += lowerName -> prop
	            _properties :+= prop
                informListeners(PropertyAdded(prop))
        }
    }
    /**
     * Adds a number of attributes to this model
     * @param attributes The attributes added to this model
     */
    def ++=(attributes: IterableOnce[V]) = attributes.iterator.foreach { this += _ }
    
    /**
     * Removes a property from this model
     * @param prop The property that is to be removed from this model
     */
    def -=(prop: V) = {
	    _properties.findIndexOf(prop).foreach { index =>
		    propMap -= prop.name.toLowerCase
		    _properties = _properties.withoutIndex(index)
		    
		    stopListeningTo(prop)
		    lazy val event = PropertyRemoved(prop)
		    listeners.foreach { _.onPropertyChange(event) }
	    }
    }
    /**
     * Removes a property from this model
     * @param propName The name of the property to remove (case-insensitive)
     */
    def -=(propName: String) = {
        val lowerCaseName = propName.toLowerCase
        propMap.get(lowerCaseName).foreach { prop =>
	        _properties = _properties.filterNot { _ == prop }
            propMap -= lowerCaseName
	        
            stopListeningTo(prop)
            lazy val event = PropertyRemoved(prop)
            listeners.foreach { _.onPropertyChange(event) }
        }
    }
    
    /**
      * Adds a new listener to this model
      * @param listener A listener that will receive property changed events
      */
    def addListener(listener: PropertyChangeListener[V]) = listeners :+= listener
    /**
      * removes a listener from this model
      * @param listener A listener that will no longer receive property changed events from this model
      */
    def removeListener(listener: Any) = listeners = listeners.filterNot { _ == listener }
    
    /**
     * Creates an immutable version of this model by using the provided property factory
     * @param propFactory The property factory used for building the properties of the new model
     */
    @deprecated("Generative models are currently deprecated", "v2.7")
    def immutableCopyUsing(propFactory: PropertyFactory[Constant]) =
        Model.withConstants(properties.map { att => propFactory(att.name, att.value) }, propFactory)
	
	private def generateProperty(name: String, value: Value = Value.empty) = {
		val generated = propFactory(name, value)
		_properties :+= generated
		propMap += (name.toLowerCase -> generated)
		
		lazy val event = PropertyAdded(generated)
		listeners.foreach { _.onPropertyChange(event) }
		
		generated
	}
    
    private def startListeningTo(prop: V) = {
        val listener = ChangeListener[Value] { e =>
            val propEvent = PropertyValueChange(prop, e)
            listeners.foreach { _.onPropertyChange(propEvent) }
        }
        valueListeners += (prop -> listener)
        prop.addListener(listener)
    }
    private def stopListeningTo(prop: V) = {
        valueListeners.get(prop).foreach { listener =>
            prop.removeListener(listener)
            valueListeners -= prop
        }
    }
}