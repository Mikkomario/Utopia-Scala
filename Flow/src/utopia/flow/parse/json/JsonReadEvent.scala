package utopia.flow.parse.json

/**
 * JsonReadEvents are different special markers / situations that occur while parsing JSON text data
 * @author Mikko Hilpinen
 * @since 17.12.2016
 */
sealed trait JsonReadEvent
{
    def marker: Char
}

object JsonReadEvent
{
    case object ObjectStart extends JsonReadEvent { val marker = '{' }
    case object ObjectEnd extends JsonReadEvent { val marker = '}' }
    case object ArrayStart extends JsonReadEvent { val marker = '[' }
    case object ArrayEnd extends JsonReadEvent { val marker = ']' }
    case object Separator extends JsonReadEvent { val marker = ',' }
    case object Assignment extends JsonReadEvent { val marker = ':' }
    case object Quote extends JsonReadEvent { val marker = '"' }
}
