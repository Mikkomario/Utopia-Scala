package utopia.reach.component.label.image

import utopia.firmament.component.stack.{Constrainable, ConstrainableWrapper}
import utopia.firmament.context.color.VariableColorContext
import utopia.flow.view.template.eventful.Flag
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.Mixed
import utopia.reach.component.factory.contextual.ContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponent, ReachComponentWrapper}
import utopia.reach.container.wrapper.Swapper

case class LoadingOrImageLabelFactory(hierarchy: ComponentHierarchy, context: VariableColorContext,
                                      settings: ViewImageLabelSettings = ViewImageLabelSettings.default)
	extends ContextualFactory[VariableColorContext, LoadingOrImageLabelFactory] with PartOfComponentHierarchy
		with ViewImageLabelSettingsWrapper[LoadingOrImageLabelFactory]
{
	// IMPLEMENTED  ----------------------------
	
	override def self: LoadingOrImageLabelFactory = this
	
	override def withContext(context: VariableColorContext): LoadingOrImageLabelFactory = copy(context = context)
	override def withSettings(settings: ViewImageLabelSettings): LoadingOrImageLabelFactory = copy(settings = settings)
	
	override def *(mod: Double): LoadingOrImageLabelFactory = copy(settings = settings * mod)
	
	
	// OTHER    --------------------------------
	
	/**
	  * Creates a new image label that supports a separate loading state
	  * @param loadingFlag A flag that contains true while the loading state should be displayed
	  * @param constructImageLabel A function that receives a prepared image label factory and yields the
	  *                            image label to wrap
	  * @param loadingLabelConstructor A function that receives a prepared animation label factory and yields the
	  *                             loading view to wrap
	  * @return A new label that displays an image or a loading view
	  */
	def apply(loadingFlag: Flag)
	         (constructImageLabel: ContextualViewImageLabelFactory => ReachComponent with Constrainable)
	         (implicit loadingLabelConstructor: LoadingLabelConstructor) =
		new LoadingOrImageLabel(hierarchy, context, loadingFlag, settings)(constructImageLabel)
}

object LoadingOrImageLabel extends Ccff[VariableColorContext, LoadingOrImageLabelFactory]
{
	override def withContext(hierarchy: ComponentHierarchy, context: VariableColorContext): LoadingOrImageLabelFactory =
		LoadingOrImageLabelFactory(hierarchy, context)
}

/**
  * A label that may swap the displayed image into an animated loading view
  * @author Mikko Hilpinen
  * @since 03.02.2025, v1.5.1
  */
class LoadingOrImageLabel(override val hierarchy: ComponentHierarchy, context: VariableColorContext,
                          val loadingFlag: Flag, settings: ViewImageLabelSettings = ViewImageLabelSettings.default)
                         (constructImageLabel: ContextualViewImageLabelFactory => ReachComponent with Constrainable)
                         (implicit loadingLabelConstructor: LoadingLabelConstructor)
	extends ReachComponentWrapper with ConstrainableWrapper
{
	override protected val wrapped: ReachComponent with Constrainable = {
		loadingFlag.fixedValue match {
			// Case: Always or never loading => Simplifies this component
			case Some(loading) =>
				// Case: Always loading => Only constructs the loading view
				if (loading)
					loadingLabelConstructor(AnimatedImageLabel.withContext(hierarchy, context).withSettings(settings))
				// Case: Never loading => Only constructs an image label
				else
					constructImageLabel(ViewImageLabel.withContext(hierarchy, context).withSettings(settings))
					
			// Case: May change between loading and default views => Uses a view-swapper
			case None =>
				Swapper.withContext(hierarchy, context).build(Mixed)
					.apply(loadingFlag) { (factories, loading) =>
						if (loading)
							loadingLabelConstructor(factories(AnimatedImageLabel).withSettings(settings))
						else
							constructImageLabel(factories(ViewImageLabel).withSettings(settings))
					}
		}
	}
}
