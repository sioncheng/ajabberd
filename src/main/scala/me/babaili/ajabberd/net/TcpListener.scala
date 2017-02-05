package me.babaili.ajabberd.net

import me.babaili.ajabberd.util.Logger

import java.net.InetSocketAddress

import akka.actor.{Actor, Props}
import akka.io.{IO, Tcp}

/**
  * Created by chengyongqiao on 02/02/2017.
  */

object TcpListener {
    def apply(host: String, port: Int) = new TcpListener(host, port)

    case class TcpConnectionHandlerClosed(name: String)
    case class QueryConnections()
}

class TcpListener(host: String, port: Int) extends Actor {

    import TcpListener._
    import Tcp._
    import context.system

    IO(Tcp) ! Bind(self, new InetSocketAddress(host, port))


    var connectionsCount = 0


    def receive = {
        case Bound(localAddress) =>
            //
            Logger.debug("bounded", localAddress.toString())
        case CommandFailed(_: Bind) =>
            //
            Logger.error("bound failure", host + ":" + port)
            context stop self
        case Connected(remote, local) =>
            //
            connectionsCount += 1

            val connection = sender()
            val handlerName = "tch-" + remote.getHostName() + ":" + remote.getPort().toString()
            val handlerProps = Props(classOf[TcpConnectionHandler], connection, self, handlerName)
            val handler = context.actorOf(handlerProps, handlerName)
            connection ! Register(handler)

            Logger.debug("connected", handlerName + " at " + local.toString())
        case TcpConnectionHandlerClosed(name) =>
            //
            connectionsCount -= 1
            Logger.debug("closed", name)
        case QueryConnections =>
            sender() ! connectionsCount
    }
}
