import kevhtp.*
import platform.posix.*

fun main(args: Array<String>) {
  println("Starting server...")

  val server = HttpServer("0.0.0.0", 8080, 1024)

  server.handle(SIGINT, {
    server.stopGracefully()
  })

  server.handle(SIGTERM, {
    server.stopGracefully()
  })

  server.handle(SIGQUIT, {
    server.stopImmediately()
  })

  server.handle("/", { _, resp -> 
    resp.start(200)
    resp.print("Hello world!")
    resp.end()
  })

  println("Listening on 8080")
  try {
    server.start()
  } finally {
    server.free()
  }
}
