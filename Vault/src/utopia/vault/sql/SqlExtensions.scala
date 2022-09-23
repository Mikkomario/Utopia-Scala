package utopia.vault.sql

import utopia.flow.collection.value.typeless.Value

import scala.language.implicitConversions

/**
 * This object contains some extensions provided by the vault project for the creation of SQL queries
 * @author Mikko Hilpinen
 * @since 17.6.2021
 */
object SqlExtensions
{
    /**
     * Wraps a value into a condition element
     */
    implicit def valueToConditionElement[V](value: V)(implicit f: V => Value): ConditionElement =
        new ConditionValue(value)
}