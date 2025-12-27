package com.sel2in.contactsDelete

import android.content.ContentProviderOperation
import android.content.Context
import android.os.Environment
import android.provider.ContactsContract
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ContactExport(
    val displayName: String?,
    val phones: List<String>,
    val emails: List<String>,
    val notes: String?
)

object ContactsManager {
    
    /**
     * Export all contacts to JSON file
     * Queries all contacts from all accounts (Google, local, etc.)
     */
    fun exportContacts(context: Context): Boolean {
        return try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "com.sel2in.contactsDelete"
            )
            dir.mkdirs()
            val outFile = File(dir, "out.contacts")

            val cr = context.contentResolver
            val writer = outFile.bufferedWriter()

            // Query all raw contacts from all accounts
            val rawContactsCursor = cr.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                "${ContactsContract.RawContacts.DELETED}=0",
                null,
                null
            ) ?: return false

            // Collect unique contact IDs
            val contactIds = mutableSetOf<String>()
            while (rawContactsCursor.moveToNext()) {
                val contactId = rawContactsCursor.getString(0)
                if (contactId != null) {
                    contactIds.add(contactId)
                }
            }
            rawContactsCursor.close()

            var count = 0
            // Export each unique contact
            for (contactId in contactIds) {
                // Get display name
                var displayName: String? = null
                cr.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                    "${ContactsContract.Contacts._ID}=?",
                    arrayOf(contactId),
                    null
                )?.use { nameCursor ->
                    if (nameCursor.moveToFirst()) {
                        displayName = nameCursor.getString(0)
                    }
                }

                // Get phone numbers
                val phones = mutableListOf<String>()
                cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
                    arrayOf(contactId),
                    null
                )?.use { pCur ->
                    while (pCur.moveToNext()) {
                        val number = pCur.getString(0)
                        if (number != null) {
                            phones.add(number)
                        }
                    }
                }

                // Get email addresses
                val emails = mutableListOf<String>()
                cr.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                    "${ContactsContract.CommonDataKinds.Email.CONTACT_ID}=?",
                    arrayOf(contactId),
                    null
                )?.use { eCur ->
                    while (eCur.moveToNext()) {
                        val email = eCur.getString(0)
                        if (email != null) {
                            emails.add(email)
                        }
                    }
                }

                // Write contact as JSON
                val obj = ContactExport(displayName, phones, emails, null)
                writer.write(JSONObject().apply {
                    put("displayName", obj.displayName)
                    put("phones", JSONArray(obj.phones))
                    put("emails", JSONArray(obj.emails))
                }.toString())
                writer.newLine()
                count++
            }

            writer.close()
            
            Toast.makeText(context, "Exported $count contacts to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Delete all contacts from device
     */
    fun deleteAllContacts(context: Context): Boolean {
        return try {
            val deleted = context.contentResolver.delete(
                ContactsContract.RawContacts.CONTENT_URI,
                null,
                null
            )
            Toast.makeText(context, "Deleted $deleted contacts", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Delete error: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Import contacts from JSON file
     */
    fun importContacts(context: Context): Boolean {
        return try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "com.sel2in.contactsDelete/in.contacts"
            )
            
            if (!file.exists()) {
                Toast.makeText(context, "Import file not found: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                return false
            }

            var count = 0
            file.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                
                val json = JSONObject(line)

                val ops = ArrayList<ContentProviderOperation>()

                // Add raw contact
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build()
                )

                // Add display name
                json.optString("displayName")?.let { displayName ->
                    if (displayName.isNotEmpty()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(
                                    ContactsContract.Data.RAW_CONTACT_ID, 0
                                )
                                .withValue(
                                    ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                                )
                                .withValue(
                                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName
                                )
                                .build()
                        )
                    }
                }

                // Add phone numbers
                json.optJSONArray("phones")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(
                                    ContactsContract.Data.RAW_CONTACT_ID, 0
                                )
                                .withValue(
                                    ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                                )
                                .withValue(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                                    arr.getString(i)
                                )
                                .withValue(
                                    ContactsContract.CommonDataKinds.Phone.TYPE,
                                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                                )
                                .build()
                        )
                    }
                }

                // Add email addresses
                json.optJSONArray("emails")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(
                                    ContactsContract.Data.RAW_CONTACT_ID, 0
                                )
                                .withValue(
                                    ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                                )
                                .withValue(
                                    ContactsContract.CommonDataKinds.Email.ADDRESS,
                                    arr.getString(i)
                                )
                                .build()
                        )
                    }
                }

                context.contentResolver.applyBatch(
                    ContactsContract.AUTHORITY,
                    ops
                )
                count++
            }
            
            Toast.makeText(context, "Imported $count contacts from ${file.absolutePath}", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Import error: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
}
