/*
 * Run Task Gradle Plugins
 * Copyright (c) 2024 Jason Penilla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jpenilla.runtask.pluginsapi

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.NamedDomainObjectCollectionSchema
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Namer
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Rule
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import xyz.jpenilla.runtask.pluginsapi.discord.DiscordApi
import xyz.jpenilla.runtask.pluginsapi.discord.DiscordApiImpl
import xyz.jpenilla.runtask.pluginsapi.github.GitHubApi
import xyz.jpenilla.runtask.pluginsapi.github.GitHubApiImpl
import xyz.jpenilla.runtask.pluginsapi.hangar.HangarApi
import xyz.jpenilla.runtask.pluginsapi.hangar.HangarApiImpl
import xyz.jpenilla.runtask.pluginsapi.modrinth.ModrinthApi
import xyz.jpenilla.runtask.pluginsapi.modrinth.ModrinthApiImpl
import xyz.jpenilla.runtask.pluginsapi.url.UrlPluginProvider
import xyz.jpenilla.runtask.pluginsapi.url.UrlPluginProviderImpl
import xyz.jpenilla.runtask.util.configure
import xyz.jpenilla.runtask.util.registerFactory
import java.util.SortedMap
import java.util.SortedSet
import javax.inject.Inject

/**
 * A set of plugins to download from various sources.
 *
 * Provides convenience methods for adding plugins from Hangar API,
 * Modrinth API, GitHub releases, and URLs.
 *
 * Combine [DownloadPluginsSpec]s using [from].
 */
public abstract class DownloadPluginsSpec @Inject constructor(
  private val registry: ExtensiblePolymorphicDomainObjectContainer<PluginApi<*, *>>
) : PolymorphicDomainObjectContainer<PluginApi<*, *>> by registry {

  @get:Nested
  public val downloads: List<PluginApiDownload>
    get() = registry.asSequence().flatMap { it.downloads }.distinct().toList()

  @get:Inject
  protected abstract val objects: ObjectFactory

  init {
    registry.registerFactory(HangarApi::class) { name -> objects.newInstance(HangarApiImpl::class, name) }
    registry.registerFactory(ModrinthApi::class) { name -> objects.newInstance(ModrinthApiImpl::class, name) }
    registry.registerFactory(GitHubApi::class) { name -> objects.newInstance(GitHubApiImpl::class, name) }
    registry.registerFactory(UrlPluginProvider::class) { name -> objects.newInstance(UrlPluginProviderImpl::class, name) }
    registry.registerFactory(DiscordApi::class) { name -> objects.newInstance(DiscordApiImpl::class, name) }

    register("hangar", HangarApi::class) {
      url.set(HangarApi.DEFAULT_URL)
    }
    register("modrinth", ModrinthApi::class) {
      url.set(ModrinthApi.DEFAULT_URL)
    }
    register("github", GitHubApi::class)
    register("url", UrlPluginProvider::class)
    register("discord", DiscordApi::class)
  }

  /**
   * Copy all downloads from the provided [DownloadPluginsSpec] into this [DownloadPluginsSpec].
   *
   * @param spec
   */
  public fun from(spec: DownloadPluginsSpec) {
    fun <T : PluginApi<*, *>> cast(o: Any): T {
      @Suppress("UNCHECKED_CAST")
      return o as T
    }
    // copy from the given spec to this spec (useful for sharing)
    for (name in spec.names) {
      val api = spec[name]
      val type = when (api) {
        is HangarApi -> HangarApi::class
        is ModrinthApi -> ModrinthApi::class
        is GitHubApi -> GitHubApi::class
        is UrlPluginProvider -> UrlPluginProvider::class
        is DiscordApi -> DiscordApi::class
        else -> throw IllegalStateException("Unknown PluginApi type: ${api.javaClass.name}")
      }
      configure(name, type) {
        copyConfiguration(cast(api))
      }
    }
  }

  // hangar extensions

  /**
   * Access to the built-in [HangarApi].
   */
  @get:Internal
  public val hangar: NamedDomainObjectProvider<HangarApi>
    get() = named("hangar", HangarApi::class)

  /**
   * Add a plugin download.
   *
   * @param plugin plugin name on Hangar
   * @param version plugin version
   */
  public fun hangar(plugin: String, version: String) {
    hangar.configure { add(plugin, version) }
  }

  // modrinth extensions

  /**
   * Access to the built-in [ModrinthApi].
   */
  @get:Internal
  public val modrinth: NamedDomainObjectProvider<ModrinthApi>
    get() = named("modrinth", ModrinthApi::class)

  /**
   * Add a plugin download.
   *
   * @param id plugin id on Modrinth
   * @param version plugin version id
   */
  public fun modrinth(id: String, version: String) {
    modrinth.configure { add(id, version) }
  }

  // github extensions

  /**
   * Access to the built-in [GitHubApi].
   */
  @get:Internal
  public val github: NamedDomainObjectProvider<GitHubApi>
    get() = named("github", GitHubApi::class)

  /**
   * Add a release artifact plugin download.
   *
   * @param owner repo owner
   * @param repo repo name
   * @param tag release tag
   * @param assetName asset file name
   */
  public fun github(owner: String, repo: String, tag: String, assetName: String) {
    github.configure { add(owner, repo, tag, assetName) }
  }

  // discord extensions

  /**
   * Access to the built-in [DiscordApi].
   */
  @get:Internal
  public val discord: NamedDomainObjectProvider<DiscordApi>
    get() = named("discord", DiscordApi::class)

  /**
   * Add a plugin download from a Discord message link.
   *
   * @param channelId the Discord channel ID where the message is located
   * @param messageId the Discord message ID containing the plugin download link
   * @param token the Discord bot token to use for fetching the message
   */
  public fun discord(channelId: String, messageId: String, token: String) {
    discord.configure { add(channelId, messageId, token) }
  }

  // url extensions

  /**
   * Access to the built-in [UrlPluginProvider].
   */
  @get:Internal
  public val url: NamedDomainObjectProvider<UrlPluginProvider>
    get() = named("url", UrlPluginProvider::class)

  /**
   * Add a plugin download.
   *
   * @param urlString download URL
   */
  public fun url(urlString: String) {
    url.configure { add(urlString) }
  }

  // All zero-arg methods must be annotated or Gradle will think it's an input
  @Internal
  override fun getAsMap(): SortedMap<String, PluginApi<*, *>> = registry.asMap

  @Internal
  override fun getNames(): SortedSet<String> = registry.names

  @Internal
  override fun getRules(): MutableList<Rule> = registry.rules

  @Internal
  override fun getCollectionSchema(): NamedDomainObjectCollectionSchema = registry.collectionSchema

  @Internal
  override fun isEmpty(): Boolean = registry.isEmpty()

  @Internal
  override fun getNamer(): Namer<PluginApi<*, *>> = registry.namer

  @get:Internal
  override val size: Int
    get() = registry.size
}
