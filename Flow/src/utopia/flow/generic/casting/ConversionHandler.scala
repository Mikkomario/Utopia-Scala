package utopia.flow.generic.casting

import utopia.flow.collection.mutable.GraphNode
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.error.DataTypeException
import utopia.flow.generic.model.immutable.{Conversion, Value}
import utopia.flow.generic.model.mutable.DataType

import scala.collection.mutable

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
	
	
	// INITIAL CODE --------------------
	
	addCaster(BasicValueCaster)
	
	
	// OTHER METHODS    ----------------
	
	/**
	  * Introduces a new value caster to be used by the conversion handler
	  * @param caster The new caster that is introduced
	  */
	def addCaster(caster: ValueCaster) = caster.conversions.foreach { addConversion(_, caster) }
	
	/**
	  * Casts a value to the desired data type (or any of the target type's sub types)
	  * @param value  The source value that is being casted
	  * @param toType The desired target data type
	  * @return The value casted to the target data type or any of the sub types of the target data
	  *         type. None if the casting failed or was not possible to begin with.
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
	  * @param value       The value that is being casted
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
		else {
			// The targeted data types include the provided types, plus each of their sub-types
			val allTargetTypes = targetTypes.flatMap { datatype => datatype.subTypes :+ datatype }
			if (allTargetTypes.isEmpty) None else _cast(value, allTargetTypes)
		}
	}
	
	/**
	  * Finds the conversion route used to cast values from a single type to another
	  * @param originType The input value data type
	  * @param targetType The output value data type
	  * @return A route used to cast the value from the origin to target data type. None if no valid route was found.
	  */
	def conversionRouteBetween(originType: DataType, targetType: DataType) =
		optimalRouteTo(originType, targetType).map { route => route.steps.map { _.conversion }.toVector }
	
	private def _cast(value: Value, targetTypes: IterableOnce[DataType]) =
	{
		val routes = targetTypes.iterator.flatMap { optimalRouteTo(value.dataType, _) }.toVector
		
		// Only works if at least a single conversion was found
		if (routes.isEmpty)
			None
		else {
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
		
		if (existingConnection.forall { _.value.reliability <= conversion.reliability })
			sourceNode.setConnection(targetNode, ConversionStep(caster, conversion))
	}
	
	// Finds existing node or creates a new one
	private def nodeForType(dataType: DataType) = conversionGraph.getOrElseUpdate(dataType,
		new ConversionNode(dataType))
	
	private def optimalRouteTo(sourceType: DataType, targetType: DataType): Option[ConversionRoute] =
		optimalRoutes.getOrElseUpdate(sourceType -> targetType, {
			val origin = nodeForType(sourceType)
			val target = nodeForType(targetType)
			// Prefers direct routes
			val directEdges = origin.edgesTo(target)
			if (directEdges.nonEmpty)
				Some(ConversionRoute(Vector(directEdges.map { _.value }.minBy { _.cost })))
			else {
				// If multiple cheapest routes are found, considers the return route, also
				val routes = origin.cheapestRoutesTo(target) { _.value.cost }._1.minGroupBy { _.size }
				if (routes.size > 1) {
					// (Route, Number of irrevocable steps, return cost)
					val routesWithReturnCosts = routes.map { route =>
						val returnRoutes = route.dropRight(1)
							.map { edge => edge.end.cheapestRouteTo(origin) { _.value.cost } }
						(route, returnRoutes.count { _.isEmpty },
							returnRoutes.flatten.foldLeft(0) { _ + _.foldLeft(0) { _ + _.value.cost } })
					}
					val bestRoute = routesWithReturnCosts.minGroupBy { _._2 }.minBy { _._3 }._1
					Some(ConversionRoute(bestRoute.map { _.value }))
				}
				else
					routes.headOption.map { r => ConversionRoute(r.map { _.value }) }
			}
		})
	
	
	// NESTED CLASSES    ---------------
	
	private case class ConversionRoute(steps: Seq[ConversionStep])
	{
		// COMPUTED PROPERTIES    -----
		
		lazy val cost = steps.foldLeft(0) { _ + _.cost }
		
		/*
        lazy val reliability = {
            if (steps.isEmpty)
                NO_CONVERSION
            else
                steps.map { _.reliability }.min
        }*/
		
		
		// IMPLEMENTED METHODS    -----
		
		override def toString = if (steps.isEmpty) "Empty route" else steps.drop(1).foldLeft(
			steps.head.conversion.toString)((str, step) => s"$str => ${ step.conversion.toString }")
		
		
		// OPERATORS    ---------------
		
		// Casts the value through each of the steps
		@throws(classOf[DataTypeException])
		def apply(value: Value) = steps.foldLeft(Some(value): Option[Value]) {
			(value, step) => value.flatMap { step(_) }
		}
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
				throw DataTypeException(s"Input of ${ value.description } in conversion $conversion")
			caster.cast(value, conversion.target)
		}
	}
}
