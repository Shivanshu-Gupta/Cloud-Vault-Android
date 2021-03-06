# Cloud Vault - Android
This is the Android implementation of Framework for Cloud Storage designed as a part of my project for the Summer Undergraduate Research Award (SURA). The framework uses Fountain Codes to emulate a reliable, secure and consolidated cloud storage service over multiple cloud storages from different cloud service providers. 

Fountain codes can be used to transform a file into an effectively unlimited number of encoded chunks which could be stored on different cloud storages. At the time of retrieval, the original file can be reassembled given any subset of those chunks, as long as the total size of the chunks was little more than the size of the original file.

The framework had the following features
1. Reliable storage with minimal redundancy as the files would be available even if some cloud services failed.
2. Obfuscated nature of the encoded chunks ensured confidentiality of data.
3. A single consolidated storage could be emulated as the encoded chunks were spread across all cloud storages. 
4. Doesn't require a central server.

This Andoird application implements the framework as a service that can read/write data on a set of clouds. The features of the applications include:
1. Syncing of files between the cloud and devices.
2. Enables the user to treat multiple cloud accounts with their individual storage as a single big cloud storage.
3. Adds security features like Confidentiality and Data Availability.
4. Doesn't require a central server hence the user data is never visible to a 3rd party.
5. Supports Dropbox only.


# Configuration
First time the app is launched, the user is taken a step by step installation where the cloud accounts can be setup.
