package utopia.echo.test

import utopia.echo.controller.vastai.vllm.VastAiVllmChatExecutor
import utopia.echo.model.vastai.settings.VastAiVllmChatExecutorSettings

/**
 * Tests label-setting via settings
 * @author Mikko Hilpinen
 * @since 18.11.2026, v1.6
 */
object VastAiSettingsTest extends App
{
	private val settings = VastAiVllmChatExecutorSettings.default.withLabel("TEST")
	
	assert(settings.instanceLabel == "TEST")
	
	private val f = VastAiVllmChatExecutor.withLabel("TEST")
	
	assert(f.executorSettings.instanceLabel == "TEST")
	
	println("Success")
}
