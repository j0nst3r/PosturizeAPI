package edu.sjsu.posturize.api.firebase;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;

//import com.google.auth.oauth2.GoogleCredentials;

@Configuration
@PropertySource("classpath:application.properties")
public class FirebaseConfig {
    
	@Value("${firestore.credentialPath}")
    private String credentialPath;
    
    @Value("${firestore.projectId}")
    private String projectId;
    
    private Firestore db;
    
    private final String userCollection = "users";
    private final String analysisCollection = "analysis";
    private final String slouchCollection = "slouches";
    private final String archiveCollection = "archive";
    private final String dailyAnalysis = "daily";
    private final String weeklyAnalysis = "weekly";
    private final String monthlyAnalysis = "monthly";
    
    private final int secPerDay = 86400;
    
    //analysis/<userId>/<daily|weekly|monthly>/<day/week/month timestamp> => will get the array of analysis
    //slouches/<userId>/<archive>/<day's time stamp> => get archived data > array of time stamps and slouches
    //slouches/<userId>/<day's time stamp> => can unprocessed data > array of time stamps and slouches
    
    ///slouches/114685049190037736486/archive/day2_time_stamp
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

    //@Scheduled(cron="0 15 */1 * * *")//will run every when its :15 of every hours
    @Scheduled(cron="0 */2 * * * *")
    public void dailyAnalysis() throws InterruptedException, ExecutionException {
        
    	Date curDate = new Date();
    	log.info("Running Daily Analysis...");
        log.info("The time is now {}", dateFormat.format(curDate));
        
        //WILL QUERY FOR ALL USERS THAT HAVE SYNC'd BEFORE CURRENT TIME
        Query query = db.collection("users").whereEqualTo("needSync", true);
        List<DocumentSnapshot> docList = query.get().get().getDocuments();
        //List<DocumentSnapshot> docList = this.getCollectionDocuments("users");
        for(DocumentSnapshot doc : docList){
        	String userId = doc.getId();
        	//pulls individual data that is needed to analysis
        	DocumentSnapshot userSlouch = this.getDocument(this.slouchCollection, userId);
        	log.info("Accessing User Slouch data:" + userId + " data: " + userSlouch.getData());
        	DocumentSnapshot userAnalysis = this.getDocument(this.analysisCollection, userId);
        	log.info("Accessing User Anaylsis data:" + userId + " data: " + userAnalysis.getData());
        	
        	//archive slouch data
        	//save userSlouch.getData() into archive/<day time stamp>
        	Date yesterday = new Date((curDate.getTime()/1000/secPerDay-1)*secPerDay*1000);
        	DocumentReference docRef = db.collection(slouchCollection).document(userId).collection(archiveCollection).document(Long.toString(yesterday.getTime()/1000));
        	Map<String, Object> docData = userSlouch.getData();
        	ApiFuture<WriteResult> future = docRef.set(docData);
        	log.info("slouch data archived");
        	
        	//delete slouch data and reset
        	docRef = db.collection(slouchCollection).document(userId);
        	Map<String, Object> data = new HashMap<>();
        	data.put("times", new ArrayList<>());
        	data.put("slouches", new ArrayList<>());
        	ApiFuture<WriteResult> result = docRef.update(data);
        	log.info("slouch data resetted");
        	
        	//perform analysis
        	ArrayList<Object> analysisArray = this.performDailyAnalysis(userSlouch);
        	
        	
        	//archive most recent analysis
        	docRef = db.collection(analysisCollection).document(userId).collection(dailyAnalysis).document(Long.toString(yesterday.getTime()/1000));
        	data = new HashMap<>();
        	data.put("result", userAnalysis.getData().get(dailyAnalysis));
        	future = docRef.set(data);
        	log.info("old analysis archived");
        	
        	//update new analysis
        	docRef = db.collection(analysisCollection).document(userId);
        	data = new HashMap<>();
        	data.put("daily", analysisArray);
        	result = docRef.update(data);
        	log.info("analysis data updated");
        	
        	docRef = db.collection("users").document(userId);
        	data = new HashMap<>();
        	data.put("needSync", false);
        	result = docRef.update(data);
        	log.info("users status updated");
        }
        
        log.info("Daily Analysis Finished");    	
    }
    
    //This method will be given a collection name and return all documents within the collection
    private List<DocumentSnapshot> getCollectionDocuments(String collection) throws InterruptedException, ExecutionException{
    	Query query = db.collection(collection);
    	ApiFuture<QuerySnapshot> collections = query.get();
    	return collections.get().getDocuments();
    }
    
    //This method will be given a collection name and return all documents within the collection
    private DocumentSnapshot getDocument(String collection, String documentId) throws InterruptedException, ExecutionException{
    	DocumentReference docRef = db.collection(collection).document(documentId);
    	ApiFuture<DocumentSnapshot> doc = docRef.get();
    	return doc.get();
    }
    
    //perform the analysis on the given set of data
    private ArrayList<Object> performDailyAnalysis(DocumentSnapshot userSlouch){
    	Map<String, Object> dataMap = userSlouch.getData();
    	ArrayList<Object> results = new ArrayList<>();
    	
    	results.add("HI THERE");
    	results.add("JUST SOME HARD CODED THINGS FOR TESTING");
    	results.add("GOODBYE");
    	
    	return results;
    }
    
    
    /*
     * HOW TO CREATE A NEW DOCUMENT
     * 
        DocumentReference docRef = db.collection("users").document("jc-test");
	    // Add document data  with id "alovelace" using a hashmap
	    Map<String, Object> data = new HashMap<>();
	    data.put("first", "J");
	    data.put("last", "C");
	    data.put("born", 1989);
	    data.put("time-stamp", dateFormat.format(new Date()));
	    //asynchronously write data
	    ApiFuture<WriteResult> result = docRef.set(data);
	    // ...
	    // result.get() blocks on response
	    System.out.println("Update time : " + result.get().getUpdateTime());
     * 
     * 
     */
    
    /*
     * HOW TO DELETE A DOCUMENT/FIELDS
     * 
        ApiFuture<WriteResult> writeResult = db.collection(<name>).document(<name>).delete()
        
        DocumentReference docRef = db.collection(<col-name>).document(<doc-name>);
	    Map<String, Object> data = new HashMap<>();
	    data.put("first", FieldValue.delete());
	    ApiFuture<WriteResult> result = docRef.update(data);
     * 
     * 
     */
   
}