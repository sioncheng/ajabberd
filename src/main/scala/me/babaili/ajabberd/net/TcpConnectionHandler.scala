package me.babaili.ajabberd.net

import javax.xml.stream.events.XMLEvent

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp
import me.babaili.ajabberd.xml.XmlTokenizer
import me.babaili.ajabberd.util.Logger

/**
  * Created by chengyongqiao on 03/02/2017.
  */
class TcpConnectionHandler(connection: ActorRef, tcpListener: ActorRef, name: String) extends Actor {
    import Tcp._

    val xmlTokenizer = new XmlTokenizer()

    def receive = {
        case Received(data) =>
            //
            val result = xmlTokenizer.decode(data.toArray)
            result match {
                case Left(xmlEvents) =>
                    //
                    expectStartStream(xmlEvents)
                case Right(exception) =>
                    //
                    processXmlStreamException(exception)
            }
        case PeerClosed =>
            //
            Logger.debug("tcp connection handler", "peer closed")
            processClose()
    }

    def expectStartStream(xmlEvents: List[XMLEvent]): Unit = {
        //
    }

    def processXmlStreamException(e: Exception): Unit = {
        //
        Logger.error("tcp connection handler received wrong xml data", e.getMessage())
        connection ! Close
        tcpListener ! TcpListener.TcpConnectionHandlerClosed(name)
        context.stop(self)
    }

    def processClose(): Unit = {
        tcpListener ! TcpListener.TcpConnectionHandlerClosed(name)
        context.stop(self)
    }
}
