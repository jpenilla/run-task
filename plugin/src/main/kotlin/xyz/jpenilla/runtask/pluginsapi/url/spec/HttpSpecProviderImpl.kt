package xyz.jpenilla.runtask.pluginsapi.url.spec

public open class HttpSpecProviderImpl : HttpSpecProvider {
  override val headers: MutableMap<String, String> = mutableMapOf()

  override fun header(pair: Pair<String, String>) {
    headers[pair.first] = pair.second
  }
}
