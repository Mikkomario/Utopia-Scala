package utopia.reach.coder.util

import utopia.coder.model.scala.datatype.Reference

/**
  * Used for accessing references from Reach and from other related modules
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.0
  */
object ReachReferences
{
	// COMPUTED -------------------
	
	def firmament = Firmament
	def reach = Reach
	
	
	// NESTED   -------------------
	
	object Firmament
	{
		import ReachPackages.Firmament._
		
		lazy val stackInsets = Reference(stackModels, "StackInsets")
		lazy val baseContext = Reference(context, "BaseContext")
		lazy val colorContext = Reference(context, "ColorContext")
		lazy val textContext = Reference(context, "TextContext")
		lazy val customDrawer = Reference(templateDrawers, "CustomDrawer")
	}
	
	object Reach
	{
		import ReachPackages.Reach._
		
		lazy val contentWindowContext = Reference(context, "ReachContentWindowContext")
		lazy val componentHierarchy = Reference(hierarchies, "ComponentHierarchy")
		lazy val cff = Reference(factories, "ComponentFactoryFactory")
		lazy val ccff = Reference(factories, "FromContextComponentFactoryFactory")
		lazy val fromContextFactory = Reference(factories, "FromContextFactory")
		lazy val framedFactory = Reference(factories, "FramedFactory")
		lazy val customDrawableFactory = Reference(factories, "CustomDrawableFactory")
		lazy val baseContextualFactory = Reference(contextualFactories, "BaseContextualFactory")
		lazy val colorContextualFactory = Reference(contextualFactories, "ColorContextualFactory")
		lazy val textContextualFactory = Reference(contextualFactories, "TextContextualFactory")
		lazy val contentWindowContextualFactory = Reference(contextualFactories, "ReachContentWindowContextualFactory")
	}
}
