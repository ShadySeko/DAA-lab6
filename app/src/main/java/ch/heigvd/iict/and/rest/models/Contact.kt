package ch.heigvd.iict.and.rest.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*




@Entity
data class Contact(@PrimaryKey(autoGenerate = true) var id: Long? = null,
              var name: String,
              var firstname: String?,
              var birthday : Calendar?,
              var email: String?,
              var address: String?,
              var zip: String?,
              var city: String?,
              var type: PhoneType?,
              var phoneNumber: String?,

              var localState: local_state = local_state.CREATED,
              var server: Long? = null,

){
    enum class local_state{
        CREATED,
        UPDATED,
        DELETED,
        IN_SYNC
    }

    val is_synchronised get() = localState == local_state.IN_SYNC
    //Set synchronised
    fun setSynchronised(){
        localState = local_state.IN_SYNC
    }
}

//The server sends back a particular form of contact, incompatible with our local form, we need to convert it

data class ServerContact(
    var id: Long?,
    var name: String,
    var firstname: String?,
    var birthday : Calendar?,
    var email: String?,
    var address: String?,
    var zip: String?,
    var city: String?,
    var type: PhoneType?,
    var phoneNumber: String?
)

fun Contact.toServerContact(): ServerContact = ServerContact(
    id = this.server,
    name = this.name,
    firstname = this.firstname,
    birthday = this.birthday,
    email = this.email,
    address = this.address,
    zip = this.zip,
    city = this.city,
    type = this.type,
    phoneNumber = this.phoneNumber
)

fun ServerContact.toContact(localId: Long? = null,
    localState: Contact.local_state = Contact.local_state.IN_SYNC): Contact = Contact(
    id = localId,
    server = id,
    localState = localState,
    name = name,
    firstname = firstname,
    birthday = birthday,
    email = email,
    address = address,
    zip = zip,
    city = city,
    type = type,
    phoneNumber = phoneNumber,
)
