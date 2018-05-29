package kevhtp

import evhtp.*
import event.*
import platform.posix.*
import kotlinx.cinterop.*

typealias HttpHandler = (HttpRequest, HttpResponse) -> Unit
typealias SignalHandler = () -> Unit

val EV_SIGNAL : Short = 0x08
val EV_PERSIST : Short = 0x10

class HttpRequest(val req : CPointer<evhtp_request_t>?) {

}

class HttpResponse(val req : CPointer<evhtp_request_t>?) {
  val buffer = evbuffer_new()

  fun start(responseCode : Short) {
    evhtp_send_reply_start(req, responseCode)
  }

  fun print(s : String) {
    evbuffer_add_printf(buffer, s)
    evhtp_send_reply_body(req, buffer)
  }

  fun end() {
    evhtp_send_reply_end(req)
  }

  fun free() {
    evbuffer_free(buffer)
  }
}

class HttpServer(val address : String, val port : Short, val backlog : Int) {
  val evbase = event_base_new()
  val htp = evhtp_new(evbase, null)
  val events = mutableListOf<CPointer<event>>()

  init {
    handle(SIGINT, { stopGracefully() })
    handle(SIGTERM, { stopGracefully() })
    handle(SIGQUIT, { stopGracefully() })
  }

  fun handle(s : Int, signalHandler : SignalHandler) {
    val handlerRef = StableRef.create(signalHandler)

    val event = event_new(evbase, s, EV_SIGNAL or EV_PERSIST, staticCFunction {
      _, _, userdata ->
      val handler = userdata!!.asStableRef<SignalHandler>().get()
      handler()
    }, handlerRef.asCPointer())

    event_add(event, null)
    events.add(event!!)
  }

  fun handle(path : String, httpHandler : HttpHandler) {
    val handlerRef = StableRef.create(httpHandler)

    evhtp_set_cb(htp, path, staticCFunction {
      req, userdata ->

      konan.initRuntimeIfNeeded()
      evhtp_request_set_keepalive(req, 0)

      val httpRequest = HttpRequest(req)
      val httpResponse = HttpResponse(req)

      try {
        val handler = userdata!!.asStableRef<HttpHandler>().get()
        handler(httpRequest, httpResponse)
      } finally {
        httpResponse.free()
      }

    }, handlerRef.asCPointer())
  }

  fun start() {
    evhtp_bind_socket(htp, address, port, backlog)
    event_base_loop(evbase, 0)
  }

  fun stopGracefully() {
    event_base_loopexit(evbase!!.reinterpret<evbase_t>(), null)
    evhtp_unbind_socket(htp)
  }

  fun stopImmediately() {
    event_base_loopbreak(evbase!!.reinterpret<evbase_t>())
    evhtp_unbind_socket(htp)
  }

  fun free() {
    for (event in events) {
      event_del(event)
      event_free(event)
    }
    evhtp_free(htp)
    event_base_free(evbase)
  }
}
