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
	
	def paradigm = Paradigm
	def firmament = Firmament
	def reach = Reach
	
	
	// NESTED   -------------------
	
	object Paradigm
	{
		import ReachPackages.Paradigm._
		
		lazy val alignment = Reference(enumerations, "Alignment")
	}
	
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
		
		lazy val componentLike = Reference(template, "ReachComponentLike")
		lazy val partOfHierarchy = Reference(template, "PartOfComponentHierarchy")
		lazy val componentHierarchy = Reference(hierarchies, "ComponentHierarchy")
		
		lazy val cff = Reference(factories, "ComponentFactoryFactory")
		lazy val ccff = Reference(factories, "FromContextComponentFactoryFactory")
		lazy val vccff = Reference(factories, "FromVariableContextComponentFactoryFactory")
		lazy val fromContextFactory = Reference(factories, "FromContextFactory")
		lazy val fromVariableContextFactory = Reference(factories, "FromVariableContextFactory")
		lazy val fromGenericContextFactory = Reference(factories, "FromGenericContextFactory")
		lazy val framedFactory = Reference(factories, "FramedFactory")
		lazy val focusListenableFactory = Reference(factories, "FocusListenableFactory")
		lazy val fromAlignmentFactory = Reference(factories, "FromAlignmentFactory")
		lazy val customDrawableFactory = Reference(factories, "CustomDrawableFactory")
		
		lazy val baseContextualFactory = Reference(contextualFactories, "BaseContextualFactory")
		lazy val colorContextualFactory = Reference(contextualFactories, "ColorContextualFactory")
		lazy val textContextualFactory = Reference(contextualFactories, "TextContextualFactory")
		lazy val contentWindowContextualFactory = Reference(contextualFactories, "ReachContentWindowContextualFactory")
		lazy val variableContextualFactory = Reference(contextualFactories, "VariableContextualFactory")
		
		lazy val wrapperContainerFactory = Reference(wrapperContainers, "WrapperContainerFactory")
		lazy val contextualWrapperContainerFactory = Reference(wrapperContainers, "ContextualWrapperContainerFactory")
		lazy val nonContextualWrapperContainerFactory = Reference(wrapperContainers, "NonContextualWrapperContainerFactory")
		lazy val combiningContainerFactory = Reference(multiContainers, "CombiningContainerFactory")
		lazy val contextualCombiningContainerFactory = Reference(multiContainers, "ContextualCombiningContainerFactory")
		lazy val nonContextualCombiningContainerFactory = Reference(multiContainers, "NonContextualCombiningContainerFactory")
		lazy val viewContainerFactory = Reference(multiContainers, "ViewContainerFactory")
		lazy val contextualViewContainerFactory = Reference(multiContainers, "ContextualViewContainerFactory")
		lazy val nonContextualViewContainerFactory = Reference(multiContainers, "NonContextualViewContainerFactory")
		
		lazy val focusListener = Reference(focus, "FocusListener")
	}
}
