package me.babaili.ajabberd.xmpp

import akka.actor.{Actor, ActorRef}
import me.babaili.ajabberd.auth.MySaslServer
import me.babaili.ajabberd.protocol.{message, _}
import me.babaili.ajabberd.util
import me.babaili.ajabberd.global.ApplicationContext
import sun.misc.{BASE64Decoder, BASE64Encoder}

import scala.collection.immutable.HashMap


/**
  * Created by cyq on 14/03/2017.
  */

class UnexpectedPacketException(val message: String) extends Exception {
    override def getMessage: String = message
}

object XmppStreamConnection {
    val INIT = 0
    val EXPECT_NEW_STREAM = 1
    val EXPECT_NEW_STREAM2 = 2
}

class XmppStreamConnection extends Actor with util.Logger {

    import XmppStreamConnection._

    var status = INIT
    var saslServer = new MySaslServer()
    var clientConnection: ActorRef = null
    var jid: String = ""
    var uid: String = ""

    def receive = {
        case packets: List[Packet] =>
            debug(s"received packets ${packets.length}")
            handlePackets(packets)
        case XmppEvents.Response(action, data) =>
            action match {
                case XmppEvents.IqResult =>
                    clientConnection ! data
                case x: Any =>
                    warn(s"what ? ${x}")
            }
        case ja @ XmppEvents.JidAssign(jidValue, uidValue) =>
            jid = jidValue
            uid = uidValue
            clientConnection ! ja
        case _ =>
            error("what message ?????")
    }

    def handlePackets(packets: List[Packet]): Unit = {
        packets.head match {
            case NullPacket =>
                warn("no packet has been emit")
            case _ =>
                var supported: Boolean = true


                if (packets.head.isInstanceOf[StreamHead]) {
                    val streamHead = packets.head.asInstanceOf[StreamHead]
                    val fromClient = streamHead.attributes.get("from").getOrElse("")
                    uid = fromClient.split("@")(0)

                    status match {
                        case INIT =>
                            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                                "to" -> fromClient)
                            val responseHead = StreamHead("jabber:client", responseAttributes)
                            debug(s"stream head ${streamHead}")
                            sender() ! XmlHead()
                            sender() ! responseHead


                            val tls = <starttls xmlns={Tls.namespace}></starttls> //<required/>

                            val mechanism = <mechanisms xmlns={Sasl.namespace}><mechanism>
                                {SaslMechanism.Plain.toString}</mechanism>
                                <mechanism>{SaslMechanism.DiagestMD5.toString}</mechanism>
                            </mechanisms>

                            val features = Features(List(tls, mechanism))

                            sender() ! features

                            debug("accept start stream and response features")
                        case EXPECT_NEW_STREAM =>
                            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                                "to" -> fromClient)
                            val responseHead = StreamHead("jabber:client", responseAttributes)
                            debug(s"stream head ${streamHead}")
                            sender() ! XmlHead()
                            sender() ! responseHead

                            val mechanism = <mechanisms xmlns={Sasl.namespace}>
                                <mechanism>{SaslMechanism.Plain.toString}</mechanism>
                                <mechanism>{SaslMechanism.DiagestMD5.toString}</mechanism>
                            </mechanisms>

                            val comprehension = <compression xmlns="http://jabber.org/features/compress"><method>zlib</method></compression>

                            val features = Features(List(mechanism, comprehension))

                            sender() ! features

                            debug("accept new stream and response features")

                        case EXPECT_NEW_STREAM2 =>
                            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                                "id" -> fromClient,
                                "version" -> "1.0")
                            val responseHead = StreamHead("jabber:client", responseAttributes)
                            debug(s"stream head 2 ${streamHead}")

                            sender() ! XmlHead()

                            sender() ! responseHead

                            val bind = <bind xmlns="urn:ietf:params:xml:ns:xmpp-bind"/>
                            val session = <session xmlns="urn:ietf:params:xml:ns:xmpp-session"/>
                            val register = <register xmlns='http://jabber.org/features/iq-register'/>
                            val features = Features(List(bind, session, register))
                            sender() ! features

                            debug("accept new stream 2 and response features")

                        //this cant be received by client, why?
                    }


                } else if (packets.head.isInstanceOf[StartTls]) {
                    val proceed = TlsProceed()
                    sender() ! proceed
                    status = EXPECT_NEW_STREAM
                } else if (packets.head.isInstanceOf[SaslAuth]) {
                    val (_, response) = saslServer.evaluateResponse(null)
                    val challengeBase64 = (new BASE64Encoder()).encode(response)
                    debug(s"challengeBase64 ${challengeBase64}")

                    val challenge = SaslChallenge(challengeBase64)

                    sender() ! challenge

                } else if (packets.head.isInstanceOf[SaslResponse]) {
                    val response = packets.head.asInstanceOf[SaslResponse]
                    debug(s"response text ${response.responseText}")

                    val decodedResponseBytes = (new BASE64Decoder()).decodeBuffer(response.responseText)
                    val decodedResponseText = new String(decodedResponseBytes)
                    debug(s"decoded responseText ${decodedResponseText}")
                    val (ok, challenge) = saslServer.evaluateResponse(decodedResponseBytes)
                    ok match {
                        case true =>
                            debug("sasl success")
                            val challengeBase64 = (new BASE64Encoder()).encode(challenge)
                            sender() ! SaslSuccess(challengeBase64)
                            status = EXPECT_NEW_STREAM2
                        case false =>
                            val challengeBase64 = (new BASE64Encoder()).encode(challenge)
                            debug(s"challengeBase64 again ${challengeBase64}")
                            sender() ! SaslChallenge(challengeBase64)
                    }
                } else if (packets.head.isInstanceOf[iq.IQ]) {
                    if (clientConnection == null) {
                        clientConnection = sender()
                    }
                    val iqStanza = packets.head.asInstanceOf[iq.IQ]

                    debug(s"iq ${iqStanza.toString}")
                    ApplicationContext.getXmppServer() ! XmppEvents.Command(XmppEvents.IqBind, uid, iqStanza)
                } else if (packets.head.isInstanceOf[message.Message]) {

                    ApplicationContext.getXmppServer() ! XmppEvents.Command(XmppEvents.MessageRequest, uid, packets.head)
                } else {
                    supported = false
                    warn(s"unsupported packet now ${packets.head}")
                    sender() ! new UnexpectedPacketException("packet is not stream head")
                }

                if (supported && packets.tail != null && !packets.tail.isEmpty) {
                    handlePackets(packets.tail)
                }

        }
    }
}
