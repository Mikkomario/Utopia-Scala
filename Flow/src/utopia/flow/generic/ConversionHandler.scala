package utopia.flow.generic

import utopia.flow.datastructure.mutable.GraphNode
import utopia.flow.generic.ConversionReliability.NO_CONVERSION
import scala.collection.mutable
import utopia.flow.datastructure.immutable.Value

/**
 * This object oversees all value conversion operations
 * @author Mikko Hilpinen
 * @since 12.11.2016
 */
object ConversionHandler
{
    // TYPES    ------------------------
    
    private type ConversionNode = GraphNode[DataType, ConversionStep]
    
    
    // ATTRIBUTES    -------------------
    
    private val conversionGraph = mutable.HashMap[DataType, ConversionNode]()
    private val optimalRoutes = mutable.HashMap[(DataType, DataType), Option[ConversionRoute]]()
    
    
    // OTHER METHODS    ----------------
    
    /**
     * Introduces a new value caster to be used by the conversion handler
     * @param caster The new caster that is introduced
     */
    def addCaster(caster: ValueCaster) = caster.conversions.foreach { addConversion(_, caster) }
    
    /**
     * Casts a value to the desired data type (or any of the target type's sub types)
     * @param value The source value that is being casted
     * @param toType The desired target data type
     * @return The value casted to the target data type or any of the sub types of the target data
     * type. None if the casting failed or was not possible to begin with.
     */
    def cast(value: Value, toType: DataType) = 
    {
        // An empty value doens't need to be modified
        if (value.isEmpty)
            Some(Value.emptyWithType(toType))
        // If value is already of desired type, doesn't need to cast
        else if (value isOfType toType)
            Some(value)
        // Finds the possible ways to cast the value to the target type or any target sub type
        else
            _cast(value, toType.subTypes :+ toType)
    }
    
    /**
     * Casts the value to a value of any of the provided data types
     * @param value The value that is being casted
     * @param targetTypes The targeted data types
     * @return The value cast to one of the data types, None if casting failed or was not possible
     */
    def cast(value: Value, targetTypes: Set[DataType]) = 
    {
        // If there are no target types, no value can be produced
        if (targetTypes.isEmpty)
            None
        // Empty values needn't be modified
        else if (value.isEmpty)
            Some(Value.emptyWithType(targetTypes.head))
        // Checks if the value already is of any of the types
        else if (targetTypes.exists(value.dataType.isOfType))
            Some(value)
        else
        {
            // The targeted data types include the provided types, plus each of their sub types
            val allTargetTypes = targetTypes.flatMap { datatype => datatype.subTypes :+ datatype }
            
            if (allTargetTypes.isEmpty) None else _cast(value, allTargetTypes)
        }
    }
    
    private def _cast(value: Value, targetTypes: Traversable[DataType]) =
    {
        val routes = targetTypes.flatMap { optimalRouteTo(value.dataType, _) }
            
        // Only works if at least a single conversion was found
        if (routes.isEmpty)
            None
        else
        {
            // Casts the value using the optimal route / target type
            val bestRoute = routes.minBy { _.cost }
            bestRoute(value)
        }
    }
    
    private def addConversion(conversion: Conversion, caster: ValueCaster) =
    {
        // Optimal routes are deprecated when new connections are introduced
        optimalRoutes.clear()
        
        // Makes sure both nodes exist
        val sourceNode = nodeForType(conversion.source)
        val targetNode = nodeForType(conversion.target)
        
        // Creates a new connection if one doesn't exist yet
        // Also, if new conversion is better or as good, replaces the existing connection
        val existingConnection = sourceNode.edgeTo(targetNode)
        
        if (existingConnection.forall { _.content.reliability <= conversion.reliability })
            sourceNode.setConnection(targetNode, ConversionStep(caster, conversion))
    }
    
    // Finds existing node or creates a new one
    private def nodeForType(dataType: DataType) = conversionGraph.getOrElseUpdate(dataType,
        new ConversionNode(dataType))
    
    private def optimalRouteTo(sourceType: DataType, targetType: DataType) = 
        optimalRoutes.getOrElseUpdate(sourceType -> targetType,
            {
                val route = nodeForType(sourceType).cheapestRouteTo(nodeForType(targetType), { _.content.cost } )
                route.map { r => ConversionRoute(r.map { _.content }) }
            })
    
    
    // NESTED CLASSES    ---------------
    
    private case class ConversionRoute(steps: Seq[ConversionStep])
    {
        // COMPUTED PROPERTIES    -----
        
        lazy val cost = steps.foldLeft(0) { _ + _.cost }
        
        lazy val reliability =
        {
            if (steps.isEmpty)
                NO_CONVERSION
            else
                steps.map { _.reliability }.min
        }
        
        
        // IMPLEMENTED METHODS    -----
        
        override def toString = if (steps.isEmpty) "Empty route" else steps.drop(1).foldLeft(
                steps.head.conversion.toString)((str, step) => str + " => " + step.conversion.toString)
        
        
        // OPERATORS    ---------------
                
        // Casts the value through each of the steps
        @throws(classOf[DataTypeException])
        def apply(value: Value) = steps.foldLeft(Some(value): Option[Value]){
            (value, step) => value.flatMap { step(_) } }
    }
    
    private case class ConversionStep(caster: ValueCaster, conversion: Conversion)
    {
        // COMP. PROPS    -----------
        
        def reliability = conversion.reliability
        
        def cost = reliability.cost
        
        
        // OPERATORS    -------------
        
        @throws(classOf[DataTypeException])
        def apply(value: Value) =
        {
            if (!(value isOfType conversion.source))
                throw DataTypeException(s"Input of $value in conversion $conversion")
            caster.cast(value, conversion.target)
        }
    }
}