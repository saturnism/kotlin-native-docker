package kevhtp

import evhtp.*
import event.*
import platform.posix.*
import kotlinx.cinterop.*

typealias HttpHandler = (HttpRequest, HttpResponse) -> Unit

class HttpRequest(val req : CPointer<evhtp_request_t>?) {

}

class HttpResponse(val req : CPointer<evhtp_request_t>?) {
  val buffer = evbuffer_new()

  fun start(responseCode : Short) {
    evhtp_send_reply_start(req, responseCode);
  }

  fun print(s : String) {
    evbuffer_add_printf(buffer, s)
    evhtp_send_reply_body(req, buffer);
  }

  fun end() {
    evhtp_send_reply_end(req);
  }

  fun free() {
    evbuffer_free(buffer)
  }
}

class HttpServer(val address : String, val port : Short, val backlog : Int) {
  val evbase = event_base_new()
  val htp = evhtp_new(evbase, null)

  fun route(path : String, httpHandler : HttpHandler) {
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
  }

  fun stop() {
    event_base_loop(evbase, 0)
    evhtp_unbind_socket(htp)
  }

  fun free() {
    evhtp_free(htp)
    event_base_free(evbase)
  }

  fun loopbreak() {
    event_base_loopbreak(evbase!!.reinterpret<evbase_t>())
  }

  fun loopexit() {
    event_base_loopexit(evbase!!.reinterpret<evbase_t>(), null)
  }
}
