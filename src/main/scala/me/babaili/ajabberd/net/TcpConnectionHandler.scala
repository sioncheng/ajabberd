package me.babaili.ajabberd.net

import javax.xml.stream.events.{StartElement, XMLEvent}

import scala.concurrent.duration._
import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp
import akka.util.ByteString
import me.babaili.ajabberd.xml.{XMPPXMLTokenizer, XmlTokenizer}
import me.babaili.ajabberd.util
import com.typesafe.scalalogging.Logger
import me.babaili.ajabberd.auth.MySaslServer
import me.babaili.ajabberd.protocol._
import me.babaili.ajabberd.xmpp.XmppStreamConnection
import sun.misc.{BASE64Decoder, BASE64Encoder}


/**
  * Created by chengyongqiao on 03/02/2017.
  */

object TcpConnectionHandler {
    val INIT = -1
    val EXPECT_START_STREAM = 0
    val EXPECT_START_TLS = 1
    val EXPECT_PROCESS_TLS = 2
    val EXPECT_NEW_STREAM = 3
    val EXPECT_LOGIN = 4
    val EXPECT_CHALLENGE = 5
    val EXPECT_CHALLENGE_SUCCESS = 6
    val EXPECT_BIND = 7
    val EXPECT_SESSION = 8
    val CLOSED = 100

    val logger = Logger("me.babaili.ajabberd.net.TcpConnectionHandler")

    case class CloseAnyway()
}

class TcpConnectionHandler(tcpListener: ActorRef, name: String) extends Actor with util.Logger {
    import Tcp._
    import TcpConnectionHandler._

    var status = EXPECT_START_STREAM

    var tcpConnection: ActorRef = null
    var sslEngine: ActorRef = null
    var xmppConnection: ActorRef = null
    var remainedXmlEvents: List[XMLEvent] = List.empty

    val xmlTokenizer = new XmlTokenizer()

    val mySaslServer = new MySaslServer()

    //************************



    //************************

    def receive = oldreceive

    def newReceive:Receive = {
        case Received(data) =>
            if (tcpConnection == null) {
                tcpConnection = sender()
            }
            if (tcpConnection != null) {
                val equal = tcpConnection.equals(sender())
                debug(s"euqal ? ${equal}")
            }
            debug(s"received data ${data.decodeString("utf8")}")
            status match {
                case INIT =>
                    val result = xmlTokenizer.decode(data.toArray)
                    result match {
                        case Left(xmlEvents) =>
                            val (packets, remained) = XMPPXMLTokenizer.emit(remainedXmlEvents ::: xmlEvents)
                            remainedXmlEvents = remained
                            if (xmppConnection == null) {
                                xmppConnection = context.actorOf(Props(classOf[XmppStreamConnection]))
                            }
                            xmppConnection ! packets
                        case Right(exception) =>
                            //
                            processXmlStreamException(exception)
                    }
                case EXPECT_PROCESS_TLS | EXPECT_NEW_STREAM  =>
                    sslEngine ! SslEngine.SslData(data.toArray)
                case x =>
                    warn(s"what status ? ${x}")
            }
        case x: me.babaili.ajabberd.protocol.Packet =>
            status match {
                case INIT =>
                    tcpConnection ! Write(ByteString.fromString(x.toString()))
                    if (x.isInstanceOf[TlsProceed]) {
                        status = EXPECT_PROCESS_TLS
                        sslEngine = context.actorOf(Props(classOf[SslEngine]))
                    }
                case EXPECT_PROCESS_TLS | EXPECT_NEW_STREAM =>
                    debug(s"send wrap request ${x.toString()}")
                    sslEngine ! SslEngine.WrapRequest(x.toString().getBytes())
                case _ =>
                    warn(s"what status ${status} for send packet ${x}")
            }
        case SslEngine.WrappedData(bs) =>
            tcpConnection ! Write(ByteString.fromArray(bs))
            debug("send tls wrapped data to client")
        case SslEngine.FinishedHandshake() =>
            debug("ssl engine finished handshake")
        case SslEngine.UnwrappedData(data) =>
            debug(s"unwrapped data ${new String(data)}")
            val result = xmlTokenizer.decode(data)
            result match {
                case Left(xmlEvents) =>
                    val (packets, remained) = XMPPXMLTokenizer.emit(remainedXmlEvents ::: xmlEvents)
                    remainedXmlEvents = remained
                    xmppConnection ! packets
                case Right(exception) =>
                    //
                    processXmlStreamException(exception)
            }
    }



    def oldreceive: Receive = {
        case d @ Received(data) =>
            //
            tcpConnection = sender()
            logger.debug(s"status ${status}")
            status match {
                case EXPECT_PROCESS_TLS | EXPECT_NEW_STREAM |
                     EXPECT_LOGIN | EXPECT_CHALLENGE | EXPECT_CHALLENGE_SUCCESS  |
                     EXPECT_BIND | EXPECT_SESSION =>
                    sslEngine ! SslEngine.SslData(data.toArray)
                    logger.debug(s"forward receive data to ssl engine")
                case EXPECT_START_STREAM | EXPECT_START_TLS =>
                    val result = xmlTokenizer.decode(data.toArray)
                    result match {
                        case Left(xmlEvents) =>
                            //
                            status match {
                                case EXPECT_START_STREAM => expectStartStream(xmlEvents)
                                case EXPECT_START_TLS => expectStartTls(xmlEvents)
                            }
                        case Right(exception) =>
                            //
                            processXmlStreamException(exception)
                    }
                case CLOSED =>
                    val decodedData = data.decodeString("UTF-8")
                    logger.warn(s"closed but received data ${decodedData}")
                case x : Any => logger.warn("what? in received" + x.toString())
            }

        case PeerClosed =>
            //
            logger.debug("peer closed")
            tcpConnection ! akka.io.Tcp.Close
            import context.dispatcher
            context.system.scheduler.scheduleOnce(1 seconds, self, CloseAnyway())
            //processClose()
        case Closed =>
            logger.debug("closed")
            processClosed()
        case CloseAnyway() =>
            logger.debug("close anyway")
            processClosed()
        case SslEngine.WrappedData(bs) =>
            tcpConnection ! Write(ByteString.fromArray(bs))
            logger.debug("send tls wrapped data to client")
        case SslEngine.FinishedHandshake() =>
            status = EXPECT_NEW_STREAM
            logger.debug("ssl engine finished handshake")
        case SslEngine.UnwrappedData(bs) =>
            val decodedString = new String(bs)
            logger.debug(s"unwrapped client data ${decodedString}")
            status match {
                case EXPECT_NEW_STREAM =>
                    val result = xmlTokenizer.decode(bs)
                    result match {
                        case Left(xmlEvents) =>
                            processNewStream(xmlEvents)
                            logger.debug("process new stream")
                        case Right(exception) =>
                            //
                            //processXmlStreamException(exception)
                            logger.warn("xml exception", exception)
                    }
                case EXPECT_LOGIN =>
                    val result = xmlTokenizer.decode(bs)
                    result match {
                        case Left(xmlEvents) =>
                            processLogin(xmlEvents)
                            logger.debug("process login")
                        case Right(exception) =>
                            //
                            //processXmlStreamException(exception)
                            logger.warn("xml exception", exception)
                    }
                case EXPECT_CHALLENGE =>
                    val result = xmlTokenizer.decode(bs)
                    result match {
                        case Left(xmlEvents) =>
                            processChallenge(xmlEvents)
                            logger.debug("process challenge")
                        case Right(exception) =>
                            //
                            //processXmlStreamException(exception)
                            logger.warn("xml exception", exception)
                    }
                case EXPECT_CHALLENGE_SUCCESS =>
                    val result = xmlTokenizer.decode(bs)
                    result match {
                        case Left(xmlEvents) =>
                            processChallengeSuccess(xmlEvents)
                            logger.debug("process challenge success")
                        case Right(exception) =>
                            //
                            //processXmlStreamException(exception)
                            logger.warn("xml exception", exception)
                    }
                case EXPECT_BIND =>
                    val result = xmlTokenizer.decode(bs)
                    result match {
                        case Left(xmlEvents) =>
                            processBind(xmlEvents)
                            logger.debug("process bind")
                        case Right(exception) =>
                            //
                            //processXmlStreamException(exception)
                            logger.warn("xml exception", exception)
                    }
                case EXPECT_SESSION =>
                    val result = xmlTokenizer.decode(bs)
                    result match {
                        case Left(xmlEvents) =>
                            processSession(xmlEvents)
                            logger.debug("process expect session")
                        case Right(exception) =>
                            //
                            //processXmlStreamException(exception)
                            logger.warn("xml exception", exception)
                    }
                case x : Any => logger.debug("what status ?" + x.toString())
            }
        case x =>
            logger.warn("what? in receive " + x.toString())
    }

    def expectStartStream(xmlEvents: List[XMLEvent]): Unit = {
        //
        if (!xmlEvents.head.isStartDocument()) {
            val errorMessage = "first xml event is not start document while expect start stream"
            debug(errorMessage)
            processXmlStreamException(XmlStreamException(errorMessage))



        } else {
            val tail = xmlEvents.tail
            if (!tail.head.isStartElement()){
                val errorMessage = "second xml event is not start element while expect start stream"
                warn(errorMessage)
                processXmlStreamException(XmlStreamException(errorMessage))
            } else {
                val localPort = tail.head.asStartElement().getName().getLocalPart()
                if (! "stream".equalsIgnoreCase(localPort)){
                    val errorMessage = "second xml event is not stream while expect start stream"
                    warn(errorMessage)
                    processXmlStreamException(XmlStreamException(errorMessage))
                } else {
                    status = EXPECT_START_TLS
                    val stream = "<?xml version=\"1.0\"?>" +
                        "<stream:stream from=\"localhost\" id=\"someid\" xmlns=\"jabber:client\" " +
                        " xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\">"
                    sender() ! Write(ByteString.fromString(stream))
                    val startTls = "<stream:features> " +
                        " <starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"></starttls> " +
                        " <mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>" +
                        "    <mechanism>DIGEST-MD5</mechanism> " +
                        "    <mechanism>PLAIN</mechanism> " +
                        " </mechanisms> " +
                        " </stream:features>"
                    sender() ! Write(ByteString.fromString(startTls))
                    debug("accept start stream and response features")
                }
            }
        }
    }

    def expectStartTls(xmlEvents: List[XMLEvent]): Unit = {
        val head = xmlEvents.head
        if (!head.isStartElement()) {
            val errorMessage = "first xml event is not start document while expect start tls"
            logger.debug(errorMessage)
            processXmlStreamException(XmlStreamException(errorMessage))



        } else {
            val name = head.asStartElement().getName().getLocalPart()
            if (! "starttls".equalsIgnoreCase(name)) {
                val errorMessage = "name of first xml event is not start tls while expect start tls"
                logger.debug(errorMessage)

                if (xmlEvents.head.isStartElement()) {
                    val localPart = xmlEvents.head.asStartElement().getName().getLocalPart()
                    if ("auth".equalsIgnoreCase(localPart)) {
                        debug("process login directly")
                        status = EXPECT_LOGIN
                        sslEngine = context.actorOf(Props(classOf[DummySslEngine]))
                        processLogin(xmlEvents)
                    }
                } else {
                    logger.warn("name of first xml event is not auth neither while expect start tls")
                    processXmlStreamException(XmlStreamException(errorMessage))
                }


            } else {
                val proceed = "<proceed xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>"
                logger.debug(proceed)
                sender() ! Write(ByteString.fromString(proceed))
                status = EXPECT_PROCESS_TLS
                sslEngine = context.actorOf(Props(classOf[SslEngine]))
            }
        }

    }


    def processNewStream(xmlEvents: List[XMLEvent]): Unit = {

        val auth = "<?xml version='1.0' encoding='UTF-8'?>" +
            "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" xmlns=\"jabber:client\" " +
            "from=\"localhost\" id=\"someid\" xml:lang=\"en\" version=\"1.0\">" +
            "<stream:features>" +
                "<mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">" +
                    "<mechanism>PLAIN</mechanism>" +
                    "<mechanism>DIGEST-MD5</mechanism>" +
                    //"<mechanism>CRAM-MD5</mechanism>" +
                    //"<mechanism>ANONYMOUS</mechanism>" +
                    //"<mechanism>JIVE-SHAREDSECRET</mechanism>" +
                "</mechanisms>" +
                "<compression xmlns=\"http://jabber.org/features/compress\">" +
                    "<method>zlib</method>" +
                "</compression>" +
                //"<auth xmlns=\"http://jabber.org/features/iq-auth\"/>" +
                //"<register xmlns=\"http://jabber.org/features/iq-register\"/>" +
            "</stream:features>"

        //tcpConnection ! Write(ByteString.fromString(auth))

        sslEngine ! SslEngine.WrapRequest(auth.getBytes())

        status = EXPECT_LOGIN

    }

    def processLogin(xmlEvents: List[XMLEvent]): Unit = {

        val (_, response) =  mySaslServer.evaluateResponse(null)
        val challengeBase64 = (new BASE64Encoder()).encode(response)
        logger.debug(s"challengeBase64 ${challengeBase64}")

        val challenge = s"<challenge xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>${challengeBase64}</challenge>"

        sslEngine ! SslEngine.WrapRequest(challenge.getBytes())

        status = EXPECT_CHALLENGE
    }

    def processChallenge(xmlEvents: List[XMLEvent]): Unit = {

        xmlEvents.head.isStartElement() match {
            case true =>
                xmlEvents.tail.head.isCharacters() match {
                    case true =>
                        val responseText = xmlEvents.tail.head.asCharacters().getData()
                        logger.debug(s"responseText ${responseText}")
                        val decodedResponseBytes = (new BASE64Decoder()).decodeBuffer(responseText)
                        val decodedResponseText = new String(decodedResponseBytes)
                        logger.debug(s"decoded responseText ${decodedResponseText}")
                        val (ok, response) = mySaslServer.evaluateResponse(decodedResponseBytes)
                        ok match {
                            case true =>
                                val challengeBase64 = (new BASE64Encoder()).encode(response)
                                logger.debug(s"success challenge base64 ${challengeBase64}")

                                val success = s"<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>${challengeBase64}</success>"
                                sslEngine ! SslEngine.WrapRequest(success.getBytes())
                                status = EXPECT_CHALLENGE_SUCCESS
                            case false =>
                                val challengeBase64 = (new BASE64Encoder()).encode(response)
                                logger.debug(s"challengeBase64 ${challengeBase64}")

                                val challenge = s"<challenge xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>${challengeBase64}</challenge>"
                                sslEngine ! SslEngine.WrapRequest(challenge.getBytes())

                                status = EXPECT_CHALLENGE
                        }
                    case false =>
                        logger.warn("not characters")
                }
            case false =>
                logger.warn("not start element")
        }
    }

    def processChallengeSuccess(xmlEvents: List[XMLEvent]): Unit = {
        val bind = "<stream:stream xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' id='someid' " +
            "from='localhost' version='1.0'><stream:features><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'/>" +
            "<session xmlns='urn:ietf:params:xml:ns:xmpp-session'/>" +
        "</stream:features>"

        status = EXPECT_BIND

        sslEngine ! SslEngine.WrapRequest(bind.getBytes())

    }

    def processBind(xmlEvents: List[XMLEvent]): Unit = {
        val id =  getIdAttribute(xmlEvents.head.asStartElement())

        val jid = s"<iq type='result' id='${id}'> <bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'> " +
            "<jid>someid@localhost/Smack</jid> </bind> </iq>  "

        sslEngine ! SslEngine.WrapRequest(jid.getBytes())

        status = EXPECT_SESSION
    }

    def processSession(xmlEvents: List[XMLEvent]): Unit = {
        if(xmlEvents.head.isStartElement()) {
            val id = getIdAttribute(xmlEvents.head.asStartElement())

            val session = s"<iq from='localhost' type='result' id='${id}'/>"

            sslEngine ! SslEngine.WrapRequest(session.getBytes())

            status = EXPECT_SESSION
        }
    }


    def processXmlStreamException(e: Exception): Unit = {
        //
        error("tcp connection handler received wrong xml data: " + e.getMessage(), e)
        sender() ! Close //will receive a closed message
    }

    def processClosed(): Unit = {
        status = CLOSED
        tcpListener ! TcpListener.CloseTcpConnectionHandler(name)
        context.stop(self)
        if (sslEngine != null) {
            context.stop(sslEngine)
        }
    }

    def getIdAttribute(startElement: StartElement): String = {
        var id = ""
        val attributes = startElement.getAttributes()
        while(attributes.hasNext()) {
            val attribute = attributes.next()
            val e = attribute.asInstanceOf[javax.xml.stream.events.Attribute]
            if ("id".equalsIgnoreCase(e.getName().getLocalPart())) {
                id = e.getValue()
            }
            logger.debug(s"attribute ${attribute} ${e.getName()} ${e.getValue()}")
        }
        id
    }

}
