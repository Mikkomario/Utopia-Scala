package utopia.vault.sql

import utopia.flow.datastructure.immutable.Value
import scala.language.implicitConversions

/**
 * This object contains some extensions provided by the vault project
 * @author Mikko Hilpinen
 * @since 25.5.2017
 */
object Extensions
{
    /**
     * Wraps a value into a condition element
     */
    implicit def valueToConditionElement[T](value: T)(implicit f: T => Value): ConditionElement = new ConditionValue(value)
}