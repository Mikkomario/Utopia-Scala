package utopia.flow.generic.model.mutable

import utopia.flow.event.listener.{ChangeListener, PropertyChangeListener}
import utopia.flow.event.model.ChangeEvent
import utopia.flow.event.model.PropertyChangeEvent.{PropertyAdded, PropertyRemoved, PropertySwapped, PropertyValueChange}
import utopia.flow.generic.model.immutable.{Constant, Value}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.immutable
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.generic.factory.PropertyFactory

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
    def apply() = new MutableModel(Vector(), PropertyFactory.forVariables)
    
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
    def using[V <: Variable](propFactory: PropertyFactory[V]) = new MutableModel(Vector(), propFactory)
}

/**
 * This is a mutable implementation of the Model template
 * @author Mikko Hilpinen
 * @since 27.11.2016
 * @tparam V The type of variables used in this model
 * @param propFactory The variable generator used for generating new values on this model
 */
class MutableModel[V <: Variable](initialProps: Iterable[V], propFactory: PropertyFactory[V]) extends ModelLike[V]
{
    // ATTRIBUTES    --------------
    
    private var propMap = initialProps.map { v => v.name.toLowerCase -> v }.toMap
    private var propOrder = initialProps.map { v => v.name.toLowerCase }.toVector.distinct
    
    private var _listeners = Vector[PropertyChangeListener[V]]()
    
    // Stores property value listeners that are used for firing property change events
    private var valueListeners = Map[V, ChangeListener[Value]]()
    
    
    // COMPUTED -------------------
    
    /**
      * An immutable version of this model
      */
    def immutableCopy = immutableCopyUsing(PropertyFactory.forConstants)
    
    /**
      * The listeners that are informed of changes within this model's properties
      */
    def listeners = _listeners
    def listeners_=(newListeners: Vector[PropertyChangeListener[V]]) = {
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
    
    def propertyMap = propMap
    override protected def propertyOrder = propOrder
    
    override protected def newProperty(attName: String): V = newProperty(attName, Value.empty)
    
    
    // OTHER    ---------------
    
    /**
     * Updates the value of a single property within this model
     * @param propName The name of the updated property
     * @param value The new value assigned to the property
     */
    def update(propName: String, value: Value) = {
        // Replaces value & generates events, may generate a new attribute
        existing(propName) match {
            // Case: Existing property => updates its value
            case Some(prop) => prop.value = value
            // Case: Non-existing property => generates one
            case None =>
                val generated = newProperty(propName, value)
                lazy val event = PropertyAdded(generated)
                listeners.foreach { _.onPropertyChange(event) }
        }
    }
    
    /**
     * Updates the value of a single attribute within this model
     * @param property a name value pair that will be updated or added
     */
    @deprecated("This method's functionality may be misleading. Please consider other options", "v2.0")
    def update(property: Property): Unit = update(property.name, property.value)
    /**
     * Updates values of multiple attributes in this model
     */
    @deprecated("This method's functionality may be misleading. Please consider other options", "v2.0")
    def update(data: template.ModelLike[Property]): Unit = data.properties.foreach(update)
    
    /**
     * Adds a new property to this model.
      * If a property with the same name already exists, it is replaced with this new property
     * @param prop The property to add to this model
     */
    def +=(prop: V) = {
        val lowerName = prop.name.toLowerCase
        propMap.get(lowerName) match {
            // Case: There already exists a property with that name => replaces it (generates a property change event)
            case Some(oldProp) =>
                propMap += lowerName -> prop
                // May start listening to value changes
                if (listeners.nonEmpty) {
                    startListeningTo(prop)
                    // Generates an event, if necessary
                    val event = PropertySwapped(ChangeEvent(oldProp, prop))
                    listeners.foreach { _.onPropertyChange(event) }
                }
            // Case: Completely new attribute => generates a property added event and modifies property order as well
            case None =>
                propMap += lowerName -> prop
                propOrder :+= lowerName
                // TODO: WET WET
                if (listeners.nonEmpty) {
                    startListeningTo(prop)
                    val event = PropertyAdded(prop)
                    listeners.foreach { _.onPropertyChange(event) }
                }
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
        if (propMap.valuesIterator.contains(prop)) {
            val lowerCaseName = prop.name.toLowerCase
            propOrder = propOrder.filterNot { _ == lowerCaseName }
            propMap -= lowerCaseName
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
            propOrder = propOrder.filterNot { _ == lowerCaseName }
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
    def immutableCopyUsing(propFactory: PropertyFactory[Constant]) =
        immutable.Model.withConstants(properties.map { att => propFactory(att.name, att.value) }, propFactory)
    
    protected def newProperty(attName: String, value: Value) = {
        // In addition to creating the attribute, adds it to this model and generates an event
        val attribute = propFactory(attName, value)
        this += attribute
        attribute
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