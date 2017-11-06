# PosturizeAPI Setup
Clone repo and get credential files from you know where... and place in src/main/resources

mvn spring-boot:run to start server

/health - health check on server

/home/firestore - test firestore connection

/home/firebase - test firebase connection

if server doesnt start due to missing file then need to add the env variable GOOGLE_APPLICATION_CREDENTIALS to point to the firestore json
