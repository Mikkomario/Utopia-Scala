package utopia.flow.parse

/**
 * JSONReadEvents are different special markers / situations that occur while parsing JSON text data 
 * @author Mikko Hilpinen
 * @since 17.12.2016
 */
sealed trait JSONReadEvent
{
    def marker: Char
}

case object ObjectStart extends JSONReadEvent { val marker = '{' }
case object ObjectEnd extends JSONReadEvent { val marker = '}' }
case object ArrayStart extends JSONReadEvent { val marker = '[' }
case object ArrayEnd extends JSONReadEvent { val marker = ']' }
case object Separator extends JSONReadEvent { val marker = ',' }
case object Assignment extends JSONReadEvent { val marker = ':' }
case object Quote extends JSONReadEvent { val marker = '"' }
