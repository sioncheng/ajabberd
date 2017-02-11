package me.babaili.ajabberd.net

import javax.xml.stream.events.XMLEvent

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp
import akka.util.ByteString
import me.babaili.ajabberd.xml.XmlTokenizer
import me.babaili.ajabberd.util.Logger

/**
  * Created by chengyongqiao on 03/02/2017.
  */

object TcpConnectionHandler {
    val EXPECT_START_STREAM = 0
    val EXPECT_START_TLS = 1
}

class TcpConnectionHandler(connection: ActorRef, tcpListener: ActorRef, name: String) extends Actor {
    import Tcp._
    import TcpConnectionHandler._

    var status = EXPECT_START_STREAM

    val logTitle = "tcp connection handler " + name

    val xmlTokenizer = new XmlTokenizer()

    def receive = {
        case Received(data) =>
            //
            val result = xmlTokenizer.decode(data.toArray)
            result match {
                case Left(xmlEvents) =>
                    //
                    status match {
                        case EXPECT_START_STREAM => expectStartStream(xmlEvents)
                        case EXPECT_START_TLS => expectStartTls(xmlEvents)
                        case x : Any => Logger.debug(logTitle, "what status ?" + x.toString())
                    }
                case Right(exception) =>
                    //
                    processXmlStreamException(exception)
            }

        case PeerClosed =>
            //
            Logger.debug(logTitle, "peer closed")
            processClose()
        case Closed =>
            Logger.debug(logTitle, "closed")
            processClose()
        case x =>
            Logger.debug(logTitle, "what? " + x.toString())
    }

    def expectStartStream(xmlEvents: List[XMLEvent]): Unit = {
        //

        if (!xmlEvents.head.isStartDocument()) {
            val errorMessage = "first xml event is not start document while expect start stream"
            Logger.debug(logTitle, errorMessage)
            processXmlStreamException(XmlStreamException(errorMessage))
        } else {
            val tail = xmlEvents.tail
            if (!tail.head.isStartElement()){
                val errorMessage = "second xml event is not start element while expect start stream"
                Logger.debug(logTitle, errorMessage)
                processXmlStreamException(XmlStreamException(errorMessage))
            } else {
                val localPort = tail.head.asStartElement().getName().getLocalPart()
                if (! "stream".equalsIgnoreCase(localPort)){
                    val errorMessage = "second xml event is not stream while expect start stream"
                    Logger.debug(logTitle, errorMessage)
                    processXmlStreamException(XmlStreamException(errorMessage))
                } else {
                    status = EXPECT_START_TLS
                    val stream = "<?xml version=\"1.0\"?>" +
                        "<stream:stream from=\"bleach.com\" id=\"someid\" xmlns=\"jabber:client\" " +
                        " xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\">"
                    connection ! Write(ByteString.fromString(stream))
                    val startTls = "<stream:features> " +
                        " <starttls xmlns=\"urn:ietf:params:xml:ns: xmpp-tls\"></starttls> " +
                        " </stream:features>"
                    connection ! Write(ByteString.fromString(startTls))
                    Logger.debug(logTitle, "accept start stream and response features")
                }
            }
        }
    }

    def expectStartTls(xmlEvents: List[XMLEvent]): Unit = {

    }

    def processXmlStreamException(e: Exception): Unit = {
        //
        Logger.error(logTitle,
            "tcp connection handler received wrong xml data: " + e.getMessage())
        connection ! Close //will receive a closed message
    }

    def processClose(): Unit = {
        tcpListener ! TcpListener.CloseTcpConnectionHandler(name)
        context.stop(self)
    }
}
