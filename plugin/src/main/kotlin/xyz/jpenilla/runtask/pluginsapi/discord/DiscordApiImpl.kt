package xyz.jpenilla.runtask.pluginsapi.discord

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import xyz.jpenilla.runtask.pluginsapi.DiscordApiDownload
import javax.inject.Inject

public abstract class DiscordApiImpl @Inject constructor(private val name: String, private val objects: ObjectFactory) : DiscordApi {
  private val jobs: MutableList<DiscordApiDownload> = mutableListOf()

  override fun getName(): String = name

  override fun add(channelId: String, messageId: String, token: String) {
    val job = objects.newInstance(DiscordApiDownload::class)
    job.channelId.set(channelId)
    job.messageId.set(messageId)
    job.token.set(token)
    jobs += job
  }

  override fun copyConfiguration(api: DiscordApi) {
    jobs.addAll(api.downloads)
  }

  override val downloads: Iterable<DiscordApiDownload>
    get() = jobs
}
