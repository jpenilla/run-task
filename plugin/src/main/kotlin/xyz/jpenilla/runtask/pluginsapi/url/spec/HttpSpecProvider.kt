package xyz.jpenilla.runtask.pluginsapi.url.spec

import org.gradle.api.tasks.Input

public interface HttpSpecProvider {
  @get:Input
  public val headers: Map<String, String>

  public fun header(pair: Pair<String, String>)
}
