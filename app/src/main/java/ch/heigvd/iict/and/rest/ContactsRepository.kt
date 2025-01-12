package ch.heigvd.iict.and.rest

import CalendarTypeAdapter
import android.content.ContentValues.TAG
import android.util.Log
import ch.heigvd.iict.and.rest.database.ContactsDao
import ch.heigvd.iict.and.rest.models.Contact
import ch.heigvd.iict.and.rest.models.ServerContact
import ch.heigvd.iict.and.rest.models.toContact
import ch.heigvd.iict.and.rest.models.toServerContact
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.utils.EmptyContent.headers
import kotlinx.serialization.serializer
import java.util.UUID
import io.ktor.serialization.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.cert.X509Certificate
import java.util.Calendar
import javax.net.ssl.X509TrustManager
import io.ktor.client.engine.android.*
import java.security.SecureRandom
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext


class ContactsRepository(private val contactsDao: ContactsDao) {

    private val client = HttpClient(Android) {
        install(Logging) {
            level = LogLevel.BODY
        }
        install(ContentNegotiation) {
            gson {
                // Configure Gson
                setPrettyPrinting()
                registerTypeAdapter(Calendar::class.java, CalendarTypeAdapter())
            }
        }
        engine {
            sslManager = { httpsURLConnection ->
                httpsURLConnection.hostnameVerifier = HostnameVerifier { _, _ -> true }
                httpsURLConnection.sslSocketFactory = SSLContext.getInstance("TLS")
                    .apply {
                        init(null, arrayOf(AllCertsTrustManager()), SecureRandom())
                    }.socketFactory
            }
        }


    }

    val allContacts = contactsDao.getAllContactsLiveData()

    suspend fun enroll(): UUID? = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.get("https://daa.iict.ch/enroll")
            val responseString = response.bodyAsText()
            UUID.fromString(responseString)
        } catch (e: Exception) {
            Log.e(TAG, "Error while enrolling", e)
            null
        }
    }

    suspend fun fetchAllContacts(uuid: UUID?) = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.get("https://daa.iict.ch/contacts") {
                headers {
                    append("X-UUID", uuid.toString())
                }
            }
            val contacts: List<ServerContact> = response.body()

            contacts.forEach { contact ->

                contactsDao.insert(contact.toContact())
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error while fetching contacts", e)
            null
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        contactsDao.clearAllContacts()
    }

    suspend fun updateContact(contact: Contact, uuid: UUID?) = withContext(Dispatchers.IO){

        println("Contact about to be updated $contact")

        if(contact.id != null){
            contact.localState = Contact.local_state.UPDATED
            contactsDao.update(contact)
        }else{
            contact.localState = Contact.local_state.CREATED
            contact.id = contactsDao.insert(contact)
            println("Contact after insert $contact")
        }

        if(uuid == null){
            return@withContext
        }
        //On a update le contact en local, on doit maintenant le mettre à jour sur le serveur

        try{
            val response =
                //Si le contact existe déja sur le serveur, on l'update avec put
                if(contact.server != null){
                    client.put("https://daa.iict.ch/contacts/${contact.server!!}"){
                        headers {
                            append("X-UUID", uuid.toString())
                            append("Content-Type", "application/json")
                        }
                        setBody(contact.toServerContact())
                    }
                //Sinon on le crée avec post
                }else{
                    client.post("https://daa.iict.ch/contacts"){
                        headers {
                            append("X-UUID", uuid.toString())
                            append("Content-Type", "application/json")
                        }
                        setBody(contact.toServerContact())
                    }
                }
            //Le serveur nous renvoie le contact avec son id, on update le contact en local
            println("response = ${response.body<Contact>()}")
            val serverContact = response.body<ServerContact>()
            val registeredContact = serverContact.toContact(contact.id)
            println("registeredContact = $registeredContact")
            contactsDao.update(registeredContact)

        }catch (e: Exception){
            Log.e(TAG, "Error while updating contact", e)
        }
    }

    suspend fun deleteContact(contact: Contact, uuid: UUID?) = withContext(Dispatchers.IO){
        //On supprime le contact en local directement si il n'est pas encore sur le serveur

        println("Contact about to be deleted $contact")

        if(contact.server == null){
            contactsDao.delete(contact)
            return@withContext
        }
        contact.localState = Contact.local_state.DELETED
        contactsDao.update(contact)

        if(uuid == null){
            return@withContext
        }

        try{
            val response =
            client.delete("https://daa.iict.ch/contacts/${contact.server!!}"){
                headers {
                    append("X-UUID", uuid.toString())
                }
            }
            contactsDao.delete(contact)

            //response is empty or error if wrong id, we log if there is an error
            if(response.status.value != 204){
                Log.e("Error while deleting contact", "Error while deleting contact")
            }
        }catch (e: Exception){
            Log.e("Error while deleting contact", "Error while deleting contact", e)
        }
    }

    suspend fun refreshAll(uuid: UUID?) = withContext(Dispatchers.IO){
        if(uuid == null){
            return@withContext
        }

        //We get all not synced contacts from DAO and apply the correct action
        contactsDao.getAllContactsByState(Contact.local_state.CREATED, Contact.local_state.UPDATED, Contact.local_state.DELETED
        ).forEach { contact ->
            when (contact.localState) {
                Contact.local_state.CREATED -> {
                    updateContact(contact, uuid)
                }
                Contact.local_state.UPDATED -> {
                    updateContact(contact, uuid)
                }
                Contact.local_state.DELETED -> {
                    deleteContact(contact, uuid)
                }
                else -> {
                    //do nothing
                }
            }
        }
    }


}