package me.babaili.ajabberd.net

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import com.typesafe.scalalogging.Logger

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

    val logger = Logger("me.babaili.ajabberd.net.TcpListener")
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
            logger.debug("bounded " + localAddress.toString())
        case CommandFailed(_: Bind) =>
            //
            logger.error("bound failure " + host + ":" + port)
            context stop self
        case Connected(remote, local) =>
            //
            connectionsCount += 1

            val connection = sender
            val handlerName = "tch-" + remote.getHostName() + ":" + remote.getPort().toString()
            val handlerProps = Props(classOf[TcpConnectionHandler], self, handlerName)
            val handler = context.actorOf(handlerProps, handlerName)
            connection ! Register(handler)

            handlers.put(handlerName, handler)

            logger.debug("connected" + handlerName + " at " + local.toString())
        case CloseTcpConnectionHandler(name) =>
            //
            connectionsCount -= 1
            handlers.remove(name)
            logger.debug("tcp connection handler closed " + name)
        case QueryConnections =>
            sender ! connectionsCount
        case Stop =>
            boundSocket ! Unbind
        case Unbound =>
            logger.debug( "unbound")
            handlers.foreach((e:(String, ActorRef)) => {
                context.stop(e._2)
                logger.debug("stopped" + e._1)
            })
        case x =>
            logger.debug( "what message?" + x.toString())
    }
}
