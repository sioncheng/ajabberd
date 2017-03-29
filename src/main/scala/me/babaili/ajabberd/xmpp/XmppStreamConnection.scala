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
    val EXPECT_SASL = 1
    val EXPECT_NEW_STREAM2 = 2
}

class XmppStreamConnection extends Actor with util.Logger {

    import XmppStreamConnection._

    var status = INIT
    var saslServer = new MySaslServer()
    var clientConnection: ActorRef = null
    var jid: JID = null
    var uid: String = ""

    def receive = {
        case packets: List[Packet] =>
            debug(s"received packets ${packets.length}")
            handlePackets(packets)
        case XmppEvents.Response(action, data) =>
            action match {
                case XmppEvents.IqResult =>
                    clientConnection ! data
                case XmppEvents.MessageResponse =>
                    clientConnection ! data
                case x: Any =>
                    warn(s"what response ? ${x}")
            }
        case ja @ XmppEvents.JidAssign(jidValue, uidValue) =>
            jid = jidValue
            uid = uidValue
            clientConnection ! ja
        case x =>
            error(s"what message ????? ${x}")
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

                            val tls = <starttls xmlns={Tls.namespace}><optional/></starttls>

                            val mechanism = <mechanisms xmlns={Sasl.namespace}><mechanism>
                                {SaslMechanism.Plain.toString}</mechanism>
                                <mechanism>{SaslMechanism.DiagestMD5.toString}</mechanism>
                                <optional/>
                            </mechanisms>

                            val features = Features(List(tls, mechanism))

                            sender() ! features

                            debug("accept start stream and response features")
                        case EXPECT_SASL =>
                            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                                "to" -> fromClient)
                            val responseHead = StreamHead("jabber:client", responseAttributes)
                            debug(s"stream head ${streamHead} at ${status}")
                            sender() ! XmlHead()
                            sender() ! responseHead

                            val mechanism = <mechanisms xmlns={Sasl.namespace}>
                                <mechanism>{SaslMechanism.Plain.toString}</mechanism>
                                <mechanism>{SaslMechanism.DiagestMD5.toString}</mechanism>
                            </mechanisms>

                            val comprehension = <compression xmlns="http://jabber.org/features/compress"><method>zlib</method></compression>

                            val features = Features(List(mechanism, comprehension))

                            sender() ! features

                        case EXPECT_NEW_STREAM2 =>
                            val responseAttributes = HashMap[String, String]("from" -> "localhost",
                                "id" -> fromClient,
                                "version" -> "1.0")
                            val responseHead = StreamHead("jabber:client", responseAttributes)
                            debug(s"stream head 2 ${streamHead}")

                            sender() ! XmlHead()

                            sender() ! responseHead

                            val compression = <compression xmlns="http://jabber.org/features/compress"><method>zlib</method></compression>
                            val bind = <bind xmlns="urn:ietf:params:xml:ns:xmpp-bind"/>
                            val session = <session xmlns="urn:ietf:params:xml:ns:xmpp-session"/>
                            val register = <register xmlns='http://jabber.org/features/iq-register'/>
                            val features = Features(List(compression, bind, session, register))
                            sender() ! features

                            debug("accept new stream 2 and response features")

                        //this cant be received by client, why?

                        case _ =>
                            warn("what status ? ")
                    }


                } else if (packets.head.isInstanceOf[StartTls]) {
                    val proceed = TlsProceed()
                    sender() ! proceed
                    status = EXPECT_SASL
                } else if (packets.head.isInstanceOf[SaslAuth]) {

                    val auth = packets.head.asInstanceOf[SaslAuth]
                    auth.mechanism match {
                        case SaslMechanism.Plain =>
                            debug("plain auth")
                            sender() ! SaslSuccess(auth.value)
                            status = EXPECT_NEW_STREAM2

                        case SaslMechanism.DiagestMD5 =>
                            debug("digest md5 auth")
                            val (_, response) = saslServer.evaluateResponse(null)
                            val challengeBase64 = (new BASE64Encoder()).encode(response).replace("\r","").replace("\n","")
                            debug(s"challengeBase64 ${challengeBase64}")

                            val challenge = SaslChallenge(challengeBase64)

                            sender() ! challenge
                        case _ =>
                            warn("unknown mechanism")

                    }
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
                            val challengeBase64 = (new BASE64Encoder()).encode(challenge).replace("\r","").replace("\n","")
                            debug(s"challengeBase64 again ${challengeBase64}")
                            sender() ! SaslChallenge(challengeBase64)
                    }
                } else if (packets.head.isInstanceOf[iq.IQ]) {
                    if (clientConnection == null) {
                        clientConnection = sender()
                    }
                    val iqStanza = packets.head.asInstanceOf[iq.IQ]


                    debug(s"iq ${iqStanza.toString}")

                    iqStanza match {
                        case get @ iq.Get(oid, oto, ofrom, ext) =>
                            debug(s"get ${get}")

                            val id = oid.getOrElse("")


                            val xml = <iq type='result' id={id}>
                                <query xmlns='jabber:iq:auth'>
                                    <username>aa</username>
                                    <password/>
                                    <digest/>
                                    <resource/>
                                </query>
                            </iq>

                            /*
                            val xml = <iq from='localhost' id={id} type='error'>
                                <error code='503' type='cancel'>
                                    <service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
                                </error>
                            </iq>
                            */
                            /*
                            val xml = <iq id={id} type="error">
                                <query xmlns="jabber:iq:auth">
                                    <username>aa</username>
                                </query>
                                <error code="405" type="CANCEL">
                                    <not-allowed xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>
                                    <text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Connection must be encrypted.
                                    </text>
                                </error>
                            </iq>*/
                            val res = iq.Result(xml)
                            debug(s"response to iq get auth ${xml.toString()}")
                            sender() ! res

                        case set @ iq.Set(Some("auth_2"), oto, ofrom, ext) =>
                            val xml = <iq type='result' id='auth_2'/>
                            val res = iq.Result(xml)
                            debug(s"response to iq set auth ${xml.toString()}")
                            sender() ! res
                        case set @ iq.Set(Some(idstr), oto, ofrom, ext) =>
                            val xml = <iq type='result' id={idstr}/>
                            /*
                            val xml = <iq id={idstr} type="error">
                                <query xmlns="jabber:iq:auth">
                                    <username>aa</username>
                                </query>
                                <error code="405" type="CANCEL">
                                    <not-allowed xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>
                                    <text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Connection must be encrypted.
                                    </text>
                                </error>
                            </iq>
                            */

                            ext match {
                                case Some(ext) =>
                                    debug(s"iq set ext ${ext}")
                                case None =>

                            }

                            val res = iq.Result(xml)
                            debug(s"response to iq set auth ${xml.toString()}")
                            sender() ! res
                        case _ =>
                            if (jid == null) {
                                ApplicationContext.getXmppServer() ! XmppEvents.Command(XmppEvents.IqBind, uid, None, iqStanza)
                            } else {
                                ApplicationContext.getXmppServer() ! XmppEvents.Command(XmppEvents.IqBind, uid, Some(jid), iqStanza)
                            }
                    }


                } else if (packets.head.isInstanceOf[message.Message]) {
                    if (jid == null) {
                        ApplicationContext.getXmppServer() ! XmppEvents.Command(XmppEvents.MessageRequest, uid, None, packets.head)
                    } else {
                        ApplicationContext.getXmppServer() ! XmppEvents.Command(XmppEvents.MessageRequest, uid, Some(jid), packets.head)
                    }

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
