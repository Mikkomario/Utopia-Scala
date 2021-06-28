package utopia.vault.nosql.factory

/**
  * Factory which retrieves items that are linked to 0-n other items of a certain type
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.6
  */
trait PossiblyMultiLinkedFactory[+Parent, Child] extends MultiLinkedFactory[Parent, Child]
{
	override def isAlwaysLinked = false
}
