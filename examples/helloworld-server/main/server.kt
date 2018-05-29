import evhtp.*
import event.*
import platform.posix.*
import kotlinx.cinterop.*

val EV_SIGNAL : Short = 0x08
val EV_PERSIST : Short = 0x10

fun main(args: Array<String>) {
  println("Starting server...")

  val evbase = event_base_new();

  val quitEvent = event_new(evbase, SIGQUIT, EV_SIGNAL or EV_PERSIST, staticCFunction {
    _, _, evbase ->
    event_base_loopbreak(evbase!!.reinterpret<evbase_t>())
  }, evbase)

  val interruptEvent = event_new(evbase, SIGINT, EV_SIGNAL or EV_PERSIST, staticCFunction {
    _, _, evbase ->
    event_base_loopbreak(evbase!!.reinterpret<evbase_t>())
  }, evbase)

  val termEvent = event_new(evbase, SIGTERM, EV_SIGNAL or EV_PERSIST, staticCFunction {
    _, _, evbase ->
    event_base_loopexit(evbase!!.reinterpret<evbase_t>(), null)
  }, evbase)

  event_add(quitEvent, null)
  event_add(interruptEvent, null)
  event_add(termEvent, null)

  val htp = evhtp_new(evbase, null);

  evhtp_set_cb(htp, "/", staticCFunction {
    req, _ ->
    konan.initRuntimeIfNeeded()

    evhtp_request_set_keepalive(req, 0)
    evhtp_send_reply_start(req, 200);

    val out = evbuffer_new();

    evbuffer_add_printf(out, "Hello from Kotlin Native!")
    evhtp_send_reply_body(req, out);

    evhtp_send_reply_end(req);

    evbuffer_free(out)
  }, null)

  evhtp_bind_socket(htp, "0.0.0.0", 8080, 1024)
  println("Listening on 8080")

  event_base_loop(evbase, 0)

  println("Exiting...")
  evhtp_unbind_socket(htp)

  evhtp_free(htp)
  event_base_free(evbase)

  event_free(termEvent)
  event_free(quitEvent)
  event_free(interruptEvent)
}
