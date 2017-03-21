package me.babaili.ajabberd.net

import akka.actor.Actor

/**
 * Created by cyq on 21/03/2017.
 */
class DummySslEngine extends Actor {

    import SslEngine._

    def receive = {
        case SslData(data) =>
            sender() ! UnwrappedData(data)
        case WrapRequest(data) =>
           sender() ! WrappedData(data)
        case Close() =>
            logger.debug("close")
        case x: Any =>
            logger.warn(s"what did i get? ${x}")
}
}
