# Posturize Server
Application Server - Currently used for perform the daily analysis cron job.

Android repository: [Andriod Application](https://github.com/mattmontero/Posturize)

Posturize Wearable repository: [Wearable](https://github.com/j0nst3r/PosturizeWearable)
## Setup
Make sure local machine have Java 1.8 and also Maven installed.
1. Download repository, and both credential files
  - posturize-468-firebase-adminsdk-rc43k-1de8e932b3.json
  - Posturize-dc2852f31788.json
2. Place the two credential files in the *PosturizeAPI/src/main/resource* folder

## Install
run `mvn install` to download and install all dependency

## Run
run `mvn spring-boot:run` to launch the application server using spring-boot

## Notes
Spring Framework, Spring Boot, Java, Firestore
