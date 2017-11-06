package edu.sjsu.posturize.api.firebase;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

//import com.google.auth.oauth2.GoogleCredentials;

@Configuration
@PropertySource("classpath:application.properties")
public class FirebaseConfig {
    
	@Value("${firestore.credentialPath}")
    private String credentialPath;
    
    @Value("${firestore.projectId}")
    private String projectId;
    
    private Firestore db;
    
    @PostConstruct
	public void init() throws FileNotFoundException, IOException {
    	InputStream is = new ClassPathResource(credentialPath).getInputStream();
    	
    	FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
    			//.setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(System.getenv(credential))))
    			.setCredentials(ServiceAccountCredentials.fromStream(is))
                .setProjectId(projectId).build();
        this.db = firestoreOptions.getService();
	}
    
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Scheduled(cron="0 0 0 * * *")
    public void dailyAnalysis() throws InterruptedException, ExecutionException {
        log.info("Running Daily Analysis...");
        log.info("The time is now {}", dateFormat.format(new Date()));
        
        DocumentReference docRef = db.collection("users").document("alovelace");
	    // Add document data  with id "alovelace" using a hashmap
	    Map<String, Object> data = new HashMap<>();
	    data.put("first", "Ada");
	    data.put("last", "Lovelace");
	    data.put("born", 1815);
	    data.put("time-stamp", dateFormat.format(new Date()));
	    //asynchronously write data
	    ApiFuture<WriteResult> result = docRef.set(data);
	    // ...
	    // result.get() blocks on response
	    System.out.println("Update time : " + result.get().getUpdateTime());
    }
}