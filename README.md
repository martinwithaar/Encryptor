# Encryptor
Strong & secure encryption for Android

## Download the app
<a href='https://play.google.com/store/apps/details?id=com.bogdwellers.pinchtozoom&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' width="223" /></a>

## Overview
Encryptor is an open-source encryption solution for Android devices based on the Encryptor4j library. It supports encryption of both files and text messages and securely stores cryptographic keys using the Android keystore.

https://developer.android.com/training/articles/keystore.html

Secret keys can be shared via NFC so that multiple devices are able to decrypt the same files and/or messages.

## Adding keys
Before you can start encrypting you have to add some keys. There are multiple ways of creating keys that are discussed below. Multiple key sizes can be selected with 256-bits as default.

### Random
Creates a random key. This is the recommended way of obtaining secure cryptographic keys.

### Password
Derives a key from a password. Useful in certain cases but intrinsically less secure.

### Base64
Advanced users can provide their own cryptographic keys that are base64 encoded. This can be useful if for instance the source of randomness of the Android device is questionable.

## File encryption
Encrypted files have the '.encrypted' extension.
The block mode used for file encryption is CTR.

## Message encryption
Text messages can be encrypted and decrypted with the tap of a button. Encrypted messages are base64 encoded.
Check the clipboard checkbox to encrypt into and decrypt from the clipboard, saving you the hassle of having to copy and paste the results.
The block mode used for message encryption is GCM.

## NFC key sharing
NFC key sharing enables users to securely share keys between devices resulting in unparalleled secrecy because no key material is ever transmitted online.

Instructions:

* Make sure both the sending and receiving device(s) support NFC and have Encryptor installed
* Create a key in the key store by tapping 'Add key'
* Click the 'Share & add' button in the bottom (if it's missing your device either has NFC disabled or lacks support completely)
* A separate activity with instructions shows up
* Pair your device with the other device(s) and tap the screen when ready to transmit
* Tap the back button when done

**Note:** Sharing keys via NFC is only possible during key creation because the Android keystore can't expose already stored key material.

### Security issues
NFC transmission is currently not encrypted. This makes it easy for sniffers to intercept the transmitted key. Make sure that any possibility of sniffing is ruled out.
Expect NFC sniffing to be extremely difficult if not completely impossible from a distance of 10 meters or greater.