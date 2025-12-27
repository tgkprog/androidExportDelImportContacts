package com.sel2in.contactsDelete

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val CONTACTS_PERMISSION_CODE = 100
    
    private lateinit var exportButton: Button
    private lateinit var deleteButton: Button
    private lateinit var importButton: Button
    
    private var pendingAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        exportButton = findViewById(R.id.exportButton)
        deleteButton = findViewById(R.id.deleteButton)
        importButton = findViewById(R.id.importButton)

        exportButton.setOnClickListener {
            checkPermissionAndExecute {
                showConfirmationDialog(
                    title = getString(R.string.confirm_export_title),
                    message = getString(R.string.confirm_export_message),
                    onConfirm = {
                        ContactsManager.exportContacts(this)
                    }
                )
            }
        }

        deleteButton.setOnClickListener {
            checkPermissionAndExecute {
                showConfirmationDialog(
                    title = getString(R.string.confirm_delete_title),
                    message = getString(R.string.confirm_delete_message),
                    onConfirm = {
                        ContactsManager.deleteAllContacts(this)
                    }
                )
            }
        }

        importButton.setOnClickListener {
            checkPermissionAndExecute {
                showConfirmationDialog(
                    title = getString(R.string.confirm_import_title),
                    message = getString(R.string.confirm_import_message),
                    onConfirm = {
                        ContactsManager.importContacts(this)
                    }
                )
            }
        }
    }

    private fun checkPermissionAndExecute(action: () -> Unit) {
        if (hasContactsPermission()) {
            action()
        } else {
            pendingAction = action
            requestContactsPermission()
        }
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestContactsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS
            ),
            CONTACTS_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CONTACTS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && 
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permission granted, execute pending action
                pendingAction?.invoke()
                pendingAction = null
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_required),
                    Toast.LENGTH_LONG
                ).show()
                pendingAction = null
            }
        }
    }

    private fun showConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            .show()
    }
}
