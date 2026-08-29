package xyz.jpenilla.runtask.pluginsapi.url.spec

public open class HttpSpecProviderImpl : HttpSpecProvider {
  override val headers: MutableMap<String, String> = mutableMapOf()

  override fun header(key: String, value: String) {
    headers[key] = value
  }
}
