package network.gen
import netconfig.Node
import netconfig.Link

// We are cheating a bit here: the underlying representation of the outgoing and incoming
// links is mutable, but the client will not see it.
class GenericNode[L<:Link](
    val incomingLinks:Seq[L],
    val outgoingLinks:Seq[L]) extends Node {

}