# Contacts Delete - Android App

A native Android app for exporting, deleting, and importing contacts.
Not fully tested. Available free and as is with no gurantees or warranties.
Open source code is at https://github.com/tgkprog/androidExportDelImportContacts

## Features

- **Export Contacts**: Save all contacts to a JSON file
- **Delete All Contacts**: Remove all contacts from device
- **Import Contacts**: Restore contacts from a JSON file
- Confirmation dialogs for all actions
- Runtime permission handling for Android 13+

## Requirements

- Android 13+ (API Level 33)
- Permissions: READ_CONTACTS, WRITE_CONTACTS

## Building

```bash
./gradlew assembleDebug
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

## Installation

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## File Locations

- **Export**: `/storage/emulated/0/Download/com.sel2in.contactsDelete/out.contacts`
- **Import**: `/storage/emulated/0/Download/com.sel2in.contactsDelete/in.contacts`

## Data Format

Contacts are stored as JSON, one contact per line:

```json
{"displayName":"John Doe","phones":["+1234567890"],"emails":["john@example.com"]}
```

## Package

`com.sel2in.contactsDelete`

## License

See requirements in [req.txt](req.txt)
