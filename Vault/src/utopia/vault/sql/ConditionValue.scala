package utopia.vault.sql

import utopia.flow.collection.value.typeless.Value

/**
 * This class offers a wrapper for values so that they can be used as condition elements
 */
class ConditionValue(val value: Value) extends ConditionElement
{
    override def toSqlSegment = SqlSegment("?", Vector(value))
}