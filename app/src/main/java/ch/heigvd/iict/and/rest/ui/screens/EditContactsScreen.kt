package ch.heigvd.iict.and.rest.ui.screens
import android.widget.Toast
import androidx.compose.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.heigvd.iict.and.rest.R
import ch.heigvd.iict.and.rest.models.Contact
import ch.heigvd.iict.and.rest.models.PhoneType
import ch.heigvd.iict.and.rest.viewmodels.ContactsViewModel

/**
 * Classe représentant l'écran de modification d'un contact (fragment)
 */

//Element composable qui permet de modifier un contact
@Composable
fun EditContactsScreen(viewModel: ContactsViewModel,
                 contact: Contact?,
                 onQuit: () -> Unit) {


    var name by remember { mutableStateOf(contact?.name) }
    var firstname by remember { mutableStateOf(contact?.firstname) }
    var email by remember { mutableStateOf(contact?.email) }
    var address by remember { mutableStateOf(contact?.address) }
    var zip by remember { mutableStateOf(contact?.zip) }
    var city by remember { mutableStateOf(contact?.city) }
    var phoneNumber by remember { mutableStateOf(contact?.phoneNumber) }
    var birthday by remember { mutableStateOf(contact?.birthday) }
    var type by remember { mutableStateOf(contact?.type) }

    val context = LocalContext.current


    fun returnContact() : Contact? {
        //si contact n'existe pas on retourne null
        if(firstname.isNullOrBlank() || name.isNullOrBlank()) {
            return null
        }else {
            return Contact(
                id = contact?.id,
                name = name!!,
                firstname = firstname,
                email = email,
                address = address,
                zip = zip,
                city = city,
                phoneNumber = phoneNumber,
                birthday = birthday,
                type = type,
                server = contact?.server,
                localState = contact?.localState ?: Contact.local_state.CREATED

            )
        }
    }

    Column(modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.formular_padding)),
        verticalArrangement = Arrangement
            .spacedBy(2.dp)) {

        //Titre de l'écran qui varie en fonction de si on édite ou on crée un contact, détecté par la présence de contact
        Text(text = stringResource(contact?.let { R.string.screen_detail_title_edit } ?: R.string.screen_detail_title_new))

        //Nom
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.screen_detail_name_subtitle))},
            value = name ?: "",
            onValueChange = { name = it }
        )

        //Prénom
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.screen_detail_firstname_subtitle))},
            value = firstname ?: "",
            onValueChange = { firstname = it }
        )

        //mail
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.screen_detail_email_subtitle))},
            value = email ?: "",
            onValueChange = { email = it }
        )

        //Date de naissance (edition non nécéssaire)
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.screen_detail_birthday_subtitle))},
            //Default value for bday is 19.01.1970
            value = birthday?.toString() ?: "19.01.1970",
            onValueChange = { birthday = null }
        )

        //Adresse
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.screen_detail_address_subtitle))},
            value = address ?: "",
            onValueChange = { address = it }
        )

        //Code postal
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.screen_detail_zip_subtitle))},
            value = zip ?: "",
            onValueChange = { zip = it }
        )

        //Ville
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.screen_detail_city_subtitle))},
            value = city ?: "",
            onValueChange = { city = it }
        )


        //Type de téléphone (radio group avec 4 choix)
        Column{
            val options = PhoneType.entries.toTypedArray() //On récupère les types de téléphone
            var selected by remember { mutableStateOf(type) }
            fun setSelected(value: PhoneType) {
                selected = value
                type = value
            }

            Text(text = stringResource(R.string.screen_detail_phonetype_subtitle))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.formular_padding)).horizontalScroll(
                    rememberScrollState()
                ))
            {
                options.forEach { type ->
                    RadioButton(selected = (selected == type),
                        onClick = { setSelected(type) })
                    Text(text = type.name)
                }
            }
        }

        //Numéro de téléphone
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.screen_detail_phonenumber_subtitle))},
            value = phoneNumber ?: "",
            onValueChange = { phoneNumber = it }
        )

        //Boutons de sauvegarde, annulation et suppression, dépendant de si on édite ou on crée un contact
        Row(
            modifier = Modifier
                .padding(end = dimensionResource(R.dimen.formular_padding))
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Bouton d'annulation
            Button(onClick = { onQuit() }) {
                Text(text = stringResource(R.string.screen_detail_btn_cancel))
            }

            // Bouton de sauvegarde
            Button(onClick = {
                val newContact = returnContact()
                if (newContact == null) {
                    Toast.makeText(context, "error updating contact", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.update(newContact)
                    onQuit()
                }
            }) {
                Text(text = stringResource(R.string.screen_detail_btn_save))
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.screen_detail_btn_save)
                )
            }

            // Bouton de suppression, existe que lors de l'édition
            if (contact != null) {
                Button(onClick = {
                    val newContact = returnContact()
                    if (newContact != null) {
                        viewModel.delete(newContact)
                    }
                    onQuit()
                }) {
                    Text(text = stringResource(R.string.screen_detail_btn_delete))
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.screen_detail_btn_delete)
                    )
                }
            }
        }
    }
}






