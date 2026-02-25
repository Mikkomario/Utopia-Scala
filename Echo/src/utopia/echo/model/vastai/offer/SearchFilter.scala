package utopia.echo.model.vastai.offer

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}

object SearchFilter
{
	/**
	 * @param property Filtered property
	 * @param operator Applied operator
	 * @param value Value applied to the operator
	 * @tparam V Type of the accepted value
	 * @return A new search filter
	 */
	def combine[V](property: OfferProperty[V], operator: FilterOperator, value: V): SearchFilter =
		apply(property, operator, property.toValue(value))
}

/**
 * A filter that may be applied to search queries
 * @param property Filtered property
 * @param operator Applied operator
 * @param value Value applied to the operator
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
case class SearchFilter(property: OfferProperty[_], operator: FilterOperator, value: Value)
{
	/**
	 * @return Converts this filter into a constant that may be added to an offer search query
	 */
	def toConstant = Constant(property.key, Model.withConstants(Constant(operator.name, value)))
}
