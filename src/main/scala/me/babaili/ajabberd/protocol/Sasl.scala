package me.babaili.ajabberd
package protocol

import scala.xml._

object Sasl
{
    val namespace = "urn:ietf:params:xml:ns:xmpp-sasl"
}

object SaslAuth
{
    val tag = "auth"

    def apply(mechanism:SaslMechanism.Value, value:String):SaslAuth = apply(<auth xmlns={ Sasl.namespace } mechanism={ mechanism.toString }>{ value }</auth>)

    def apply(xml:Node):SaslAuth = new SaslAuth(xml)
}

class SaslAuth(xml:Node) extends XmlWrapper(xml) with Packet
{
    val mechanism:SaslMechanism.Value =  SaslMechanism.withName((this.xml \ "@mechanism").text)
    val value:String =  this.xml.text
}

object SaslSuccess
{
    val tag = "success"

    def apply():SaslSuccess = apply(<success xmlns={ Sasl.namespace }/>)

    def apply(text: String): SaslSuccess = apply(<success xmlns={ Sasl.namespace }>{text}</success>)

    def apply(xml:Node):SaslSuccess = new SaslSuccess(xml)
}

class SaslSuccess(xml:Node) extends XmlWrapper(xml) with Packet
{
}

object SaslAbort
{
    val tag = "abort"

    def apply():SaslAbort = apply(<abort xmlns={ Sasl.namespace }/>)

    def apply(xml:Node):SaslAbort = new SaslAbort(xml)
}

class SaslAbort(xml:Node) extends XmlWrapper(xml) with Packet
{
}

object SaslError
{
    val tag = "failure"

    def apply(condition:SaslErrorCondition.Value):SaslError =
    {
        val conditionNode = Elem(null, condition.toString, Null, TopScope)
        apply(Elem(null, tag, Null, new NamespaceBinding(null, Sasl.namespace, TopScope), conditionNode))
    }

    def apply(xml:Node):SaslError = new SaslError(xml)
}

class SaslError(xml:Node) extends XmlWrapper(xml) with Packet
{
    val condition:SaslErrorCondition.Value =
    {
        this.xml.child.length match
        {
            case 0 => throw new Exception("unknown error condition " + this.xml)
            case _ => SaslErrorCondition.withName(this.xml.child.head.label)
        }
    }
}

object SaslMechanism extends Enumeration
{
    type mechanism = Value

    // TODO: add more
    val Plain = Value("PLAIN")
    val DiagestMD5 = Value("DIGEST-MD5")
    val External = Value("EXTERNAL")
}

object SaslErrorCondition extends Enumeration
{
    type condition = Value

    val Aborted = Value("aborted")
    val AccountDisabled = Value("account-disabled")
    val CredentialsExpired = Value("credentials-expired")
    val EncryptionRequired = Value("encryption-required")
    val IncorrectEncoding = Value("incorrect-encoding")
    val InvalidAuthzid = Value("invalid-authzid")
    val InvalidMechanism = Value("invalid-mechanism")
    val MalformedRequest = Value("malformed-request")
    val MechanismTooWeak = Value("mechanism-too-weak")
    val NotAuthorized = Value("not-authorized")
    val TemporaryAuthFailure = Value("temporary-auth-failure")
    val TransitionNeeded = Value("transition-needed")
}



object SaslChallenge {
    val tag = "challenge"

    def apply(response: String): SaslChallenge = apply(<challenge xmlns={Sasl.namespace}>{response}</challenge>)
    def apply(xml: Node): SaslChallenge = new SaslChallenge(xml)
}

class SaslChallenge (xml:Node) extends XmlWrapper(xml) with Packet{

}

object SaslResponse  {
    val tag = "response"

    def apply(response: String): SaslResponse = apply(<response xmlns={Sasl.namespace}>{response}</response>)
    def apply(xml: Node): SaslResponse = new SaslResponse(xml)
}

class SaslResponse(xml: Node) extends XmlWrapper(xml) with Packet {

    val responseText = xml.text
}



