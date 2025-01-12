package ch.heigvd.iict.and.rest.viewmodels

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.heigvd.iict.and.rest.ContactsApplication
import kotlinx.coroutines.launch
import ch.heigvd.iict.and.rest.models.Contact
import java.util.UUID

class ContactsViewModel(application: ContactsApplication) : AndroidViewModel(application) {

    private val repository = application.repository

    val allContacts = repository.allContacts


    private var _contact: MutableLiveData<Contact?> = MutableLiveData(null);
    val contact: MutableLiveData<Contact?> get() = _contact
    private var _editMode: MutableLiveData<Boolean> = MutableLiveData(false);
    val editMode: MutableLiveData<Boolean> get() = _editMode


    //PREFERENCES - TO STORE THE TOKEN for persisting the UUID
    private val sharedPrefs = application.getSharedPreferences("contact_preferences", Context.MODE_PRIVATE)

    private var uuid: UUID?
        get() = sharedPrefs.getString("uuid", null)?.let { UUID.fromString(it) }
        set(value) = with(sharedPrefs.edit()) {
            putString("uuid", value.toString())
            apply()
        }



    //Permet de créer un contact, si contact est null, on crée un nouveau contact comme ca on
    //peut réutiliser le même fragment pour l'édition et la création
    fun manage_contact(contact : Contact? = null) {
        _contact.value = contact
        _editMode.value = true
    }

    fun quit_edit() {
        _editMode.value = false
        _contact.value = null
    }


    fun enroll() {
        viewModelScope.launch {
            repository.deleteAll()
            uuid = repository.enroll()
            if(uuid == null){
                return@launch
            }
            repository.fetchAllContacts(uuid!!)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshAll(uuid)
        }
    }

    fun create(contact: Contact) {
        viewModelScope.launch {
            repository.updateContact(contact, uuid)
        }
    }

    fun update(contact: Contact) {
        viewModelScope.launch {
            repository.updateContact(contact, uuid)
        }
    }

    fun delete(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact, uuid)
        }
    }

}

class ContactsViewModelFactory(private val application: ContactsApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}