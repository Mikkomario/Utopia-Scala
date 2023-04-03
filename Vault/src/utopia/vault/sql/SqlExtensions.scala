package utopia.vault.sql

import utopia.flow.generic.model.immutable.Value

import scala.language.implicitConversions

/**
 * This object contains some extensions provided by the vault project for the creation of SQL queries
 * @author Mikko Hilpinen
 * @since 17.6.2021
 */
@deprecated("These conversions are now implicitly available in the ConditionElement itself", "v1.16")
object SqlExtensions
{
    /**
     * Wraps a value into a condition element
     */
    implicit def valueToConditionElement[V](value: V)(implicit f: V => Value): ConditionElement =
        new ConditionValue(value)
}