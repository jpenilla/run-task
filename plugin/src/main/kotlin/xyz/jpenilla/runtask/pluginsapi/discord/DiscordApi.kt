package xyz.jpenilla.runtask.pluginsapi.discord

import xyz.jpenilla.runtask.pluginsapi.DiscordApiDownload
import xyz.jpenilla.runtask.pluginsapi.PluginApi

/**
 * [PluginApi] implementation for Discord message links.
 */
public interface DiscordApi : PluginApi<DiscordApi, DiscordApiDownload> {
  /**
   * Add a Discord message plugin download.
   *
   * @param channelId the ID of the Discord channel containing the message
   * @param messageId the ID of the Discord message to fetch
   * @param token the bot token to use for fetching the message
   */
  public fun add(
    channelId: String,
    messageId: String,
    token: String
  )
}
