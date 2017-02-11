package me.babaili.ajabberd.net

import me.babaili.ajabberd.util.Logger
import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp.Unbound
import akka.io.{IO, Tcp}

import scala.collection.mutable

/**
  * Created by chengyongqiao on 02/02/2017.
  */

object TcpListener {
    def logTitle = "tcp listener"

    def apply(host: String, port: Int) = new TcpListener(host, port)

    case class CloseTcpConnectionHandler(name: String)
    case class QueryConnections()
    case class Stop()
}

class TcpListener(host: String, port: Int) extends Actor {

    import TcpListener._
    import Tcp._
    import context.system

    IO(Tcp) ! Bind(self, new InetSocketAddress(host, port))


    var connectionsCount = 0
    var boundSocket: ActorRef = null
    val handlers: mutable.Map[String, ActorRef] = new mutable.HashMap[String, ActorRef]()


    def receive = {
        case Bound(localAddress) =>
            //
            boundSocket = sender()
            Logger.debug(TcpListener.logTitle, "bounded " + localAddress.toString())
        case CommandFailed(_: Bind) =>
            //
            Logger.error("bound failure", host + ":" + port)
            context stop self
        case Connected(remote, local) =>
            //
            connectionsCount += 1

            val connection = sender
            val handlerName = "tch-" + remote.getHostName() + ":" + remote.getPort().toString()
            val handlerProps = Props(classOf[TcpConnectionHandler], connection, self, handlerName)
            val handler = context.actorOf(handlerProps, handlerName)
            connection ! Register(handler)

            handlers.put(handlerName, handler)

            Logger.debug(TcpListener.logTitle,
                "connected" + handlerName + " at " + local.toString())
        case CloseTcpConnectionHandler(name) =>
            //
            connectionsCount -= 1
            handlers.remove(name)
            Logger.debug(TcpListener.logTitle, "tcp connection handler closed " + name)
        case QueryConnections =>
            sender ! connectionsCount
        case Stop =>
            boundSocket ! Unbind
        case Unbound =>
            Logger.debug(TcpListener.logTitle, "unbound")
            handlers.foreach((e:(String, ActorRef)) => {
                context.stop(e._2)
                Logger.debug(TcpListener.logTitle, "stopped" + e._1)
            })
        case x =>
            Logger.debug(TcpListener.logTitle, "what message?" + x.toString())
    }
}
