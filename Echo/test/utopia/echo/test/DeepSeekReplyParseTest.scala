package utopia.echo.test

import utopia.bunnymunch.jawn.JsonBunny
import utopia.echo.model.response.openai.BufferedOpenAiReply

/**
 * @author Mikko Hilpinen
 * @since 30.01.2026, v1.5
 */
object DeepSeekReplyParseTest extends App
{
	private val message = JsonBunny(
		"{\"choices\": [{\"finish_reason\": \"stop\", \"index\": 0, \"logprobs\": null, \"message\": {\"content\": \"你好！\uD83D\uDC4B 很高兴见到你！有什么我可以帮助你的吗？无论是回答问题、聊天，还是协助处理任务，我都很乐意为你提供帮助。请随时告诉我你需要什么！\uD83D\uDE0A\", \"role\": \"assistant\"}}], \"created\": 1769782356, \"id\": \"c8562e61-9276-4685-bed1-d5e391a7facb\", \"model\": \"deepseek-chat\", \"object\": \"chat.completion\", \"system_fingerprint\": \"fp_eaab8d114b_prod0820_fp8_kvcache\", \"usage\": {\"completion_tokens\": 40, \"prompt_cache_hit_tokens\": 0, \"prompt_cache_miss_tokens\": 5, \"prompt_tokens\": 5, \"prompt_tokens_details\": {\"cached_tokens\": 0}, \"total_tokens\": 45}}")
		.flatMap { _.tryModel.flatMap(BufferedOpenAiReply.apply) }
		.get
	
	println(s"${ message.messages.size } messages received:")
	message.messages.foreach { m =>
		println(s"- ${ m.text }")
	}
	
	println(s"\nReply text: ${ message.text }")
}
