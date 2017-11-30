package edu.sjsu.posturize.api.firebase;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    
    private static final String USER_COLLECTION = "users";
    private static final String ANALYSIS_COLLECTION = "analysis";
    private static final String SLOUCH_COLLECTION = "slouches";
    private static final String ARCHIVE_COLLECTION = "archive";
    private static final String DAILY_ANALYSIS = "daily";
    private static final String WEEKLY_ANALYSIS = "weekly";
    private static final String MONTHLY_ANALYSIS = "monthly";
    
    private static final int SEC_PER_DAY = 86400;
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    
    private static Firestore db;
    
    @PostConstruct
	public void init() throws FileNotFoundException, IOException {
    	InputStream is = new ClassPathResource(credentialPath).getInputStream();
    	
    	FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
    			//.setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(System.getenv(credential))))
    			.setCredentials(ServiceAccountCredentials.fromStream(is))
                .setProjectId(projectId).build();
        FirebaseConfig.db = firestoreOptions.getService();
	}
    
    /* 
     * @return DB object 
     */
    public static Firestore getInstance(){
    		return db;
    }
    
    

    @Scheduled(cron="0 15 */1 * * *")//will run every when its :15 of every hours
    public void dailyAnalysis() throws InterruptedException, ExecutionException {
        
    	Date curDate = new Date();
    	log.info("Running Daily Analysis...");
        log.info("The time is now {}", dateFormat.format(curDate));
        
        //WILL QUERY FOR ALL USERS THAT HAVE SYNC'd BEFORE CURRENT TIME
        Query query = db.collection("users").whereEqualTo("isSynced", true);
        List<DocumentSnapshot> docList = query.get().get().getDocuments();
        //List<DocumentSnapshot> docList = getCollectionDocuments("users");
        for(DocumentSnapshot doc : docList){
        	String userId = doc.getId();
        	//pulls individual data that is needed to analysis
        	DocumentSnapshot userSlouch = getDocument(SLOUCH_COLLECTION, userId);
        	log.info("Accessing User Slouch data:" + userId + " data: " + userSlouch.getData());
        	DocumentSnapshot userAnalysis = getDocument(ANALYSIS_COLLECTION, userId);
        	log.info("Accessing User Analysis data:" + userId + " data: " + userAnalysis.getData());
        	
        	//archive slouch data
        	//save userSlouch.getData() into archive/<day time stamp>
        	Date yesterday = new Date((curDate.getTime()/1000/SEC_PER_DAY-1)*SEC_PER_DAY*1000);
        	DocumentReference docRef = db.collection(SLOUCH_COLLECTION).document(userId).collection(ARCHIVE_COLLECTION).document(Long.toString(yesterday.getTime()/1000));
        	Map<String, Object> docData = userSlouch.getData();
        	ApiFuture<WriteResult> future = docRef.set(docData);
        	log.info("slouch data archived");
        	
        	//delete slouch data and reset
        	docRef = db.collection(SLOUCH_COLLECTION).document(userId);
        	Map<String, Object> data = new HashMap<>();
        	data.put("times", new ArrayList<>());
        	data.put("slouches", new ArrayList<>());
        	ApiFuture<WriteResult> result = docRef.update(data);
        	log.info("slouch data resetted");
        	
        	//perform analysis
        	ArrayList<Object> analysisArray = performDailyAnalysis(userSlouch);
        	
        	
        	//archive most recent analysis
        	docRef = db.collection(ANALYSIS_COLLECTION).document(userId).collection(DAILY_ANALYSIS).document(Long.toString(yesterday.getTime()/1000));
        	data = new HashMap<>();
        	data.put("result", userAnalysis.getData().get(DAILY_ANALYSIS));
        	future = docRef.set(data);
        	log.info("old analysis archived");
        	
        	//update new analysis
        	docRef = db.collection(ANALYSIS_COLLECTION).document(userId);
        	data = new HashMap<>();
        	data.put("daily", analysisArray);
        	result = docRef.update(data);
        	log.info("analysis data updated");
        	
        	//set the synced flag in user document to false
        	docRef = db.collection("users").document(userId);
        	data = new HashMap<>();
        	data.put("isSynced", false);
        	result = docRef.update(data);
        	log.info("users status updated");
        }
        
        log.info("Daily Analysis Finished");    	
    }
    
    /*
     * Force the analysis to run for specific user(disregard the normal cron job)
     * 
     * @param  userId is specified user to run the analysis on
     * @return String 'OK' if analysis executes correctl, otherwise 'ERROR'
     */
    public static String forcedAnalysis(String userId) throws InterruptedException, ExecutionException {
    	Date curDate = new Date();
    	log.info("Running Daily Analysis...");
        log.info("The time is now {}", dateFormat.format(curDate));
        
        //pulls individual data that is needed to analysis
        DocumentSnapshot userSlouch = getDocument(SLOUCH_COLLECTION, userId);
        log.info("Accessing User Slouch data:" + userId + " data: " + userSlouch.getData());
        DocumentSnapshot userAnalysis = getDocument(ANALYSIS_COLLECTION, userId);
        log.info("Accessing User Analysis data:" + userId + " data: " + userAnalysis.getData());
        
        if(userSlouch.exists() && userAnalysis.exists()){
	        //archive slouch data
	        //save userSlouch.getData() into archive/<day time stamp>
	        Date yesterday = new Date((curDate.getTime()/1000/SEC_PER_DAY-1)*SEC_PER_DAY*1000);
	        DocumentReference docRef;
	        Map<String, Object> docData;
	        ApiFuture<WriteResult> future;
	        Map<String, Object> data = new HashMap<>();
	        ApiFuture<WriteResult> result;
	        	
	        //perform analysis
	        ArrayList<Object> analysisArray = performDailyAnalysis(userSlouch);
	        	
	        //update new analysis
	        docRef = db.collection(ANALYSIS_COLLECTION).document(userId);
	        data = new HashMap<>();
	        data.put("daily", analysisArray);
	        result = docRef.update(data);
	        
	        log.info("Forced Analysis Finished");
	        return "OK";
        }else{
        	log.info("No User Found....");
	        return "ERROR";
        }
        
    }

    /*
     * provided the collection name and document Id, returns the DocumentSnapshot
     * 
     * @param collection the collection name
     * @param documentId the documentId
     * @return returns a DocumentSnapshot for the given collection/documentId
     */
    private static DocumentSnapshot getDocument(String collection, String documentId) throws InterruptedException, ExecutionException{
    	DocumentReference docRef = db.collection(collection).document(documentId);
    	ApiFuture<DocumentSnapshot> doc = docRef.get();
    	return doc.get();
    }
    
    /*
     *perform the analysis on the given set of data 
     *
     * @param userSlouch a DocumentSnapshot of the user's slouch data
     * @return an ArrayList<Object> that contains the result string of all analysis 
     */
    private static ArrayList<Object> performDailyAnalysis(DocumentSnapshot userSlouch){
    	Map<String, Object> dataMap = userSlouch.getData();
    	ArrayList<Double> slouches = (ArrayList<Double>) dataMap.get("slouches");
    	ArrayList<Date> times = (ArrayList<Date>) dataMap.get("times");
    	Double[] slouchData = new Double[slouches.size()];
    	Date[] dateData = new Date[times.size()];
    	slouchData = slouches.toArray(slouchData);
    	dateData = times.toArray(dateData);
    	ArrayList<Object> results = new ArrayList<>();
    	
    	//total number of slouches recorded today
    	int numSlouches = slouchData.length;
    	
    	//highest density slouch
    	int timeLimit = 60*60*1000;
    	int begIndex = 0;
    	int endIndex = 0;
    	
    	results.add("Total number of slouches recorded: " + numSlouches);
    	
    	results.add("HI THERE");
    	results.add("JUST SOME HARD CODED THINGS FOR TESTING");
    	results.add("GOODBYE");
    	
    	return results;
    }
}